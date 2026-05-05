#!/bin/sh
set -eu

BASE_URL="${BPMT_API_BASE_URL:-http://127.0.0.1/api}"
APP_KEY="${BPMT_API_APP_KEY:-bpmt-api}"
APP_SECRET="${BPMT_API_APP_SECRET:-bpmt-api-secret}"
CURL_INSECURE="${BPMT_API_CURL_INSECURE:-false}"
CURL_OPTS="-sS"
if [ "$CURL_INSECURE" = "1" ] || [ "$CURL_INSECURE" = "true" ]; then
  CURL_OPTS="$CURL_OPTS -k"
fi

curl $CURL_OPTS -f -I "$BASE_URL/openapi.json" | grep -q '200'
curl $CURL_OPTS -f -I "$BASE_URL/docs/" | grep -q '200'

STATUS="$(curl $CURL_OPTS -o /tmp/bpmt-api-unauthorized.json -w '%{http_code}' "$BASE_URL/v1/dynamic-tables")"
if [ "$STATUS" != "401" ]; then
  printf '%s\n' "Expected 401 without signature, got $STATUS"
  cat /tmp/bpmt-api-unauthorized.json
  exit 1
fi

signed_request() {
  METHOD="$1"
  PUBLIC_PATH="$2"
  QUERY="$3"
  BODY="$4"
  OUT="$5"
  EXPECTED_STATUS="$6"

  TIMESTAMP="$(date +%s)"
  NONCE="smoke-$$-$TIMESTAMP"
  BODY_HASH="$(printf '%s' "$BODY" | shasum -a 256 | awk '{print $1}')"
  CANONICAL="$(printf '%s\n%s\n%s\n%s\n%s\n%s' "$METHOD" "$PUBLIC_PATH" "$QUERY" "$TIMESTAMP" "$NONCE" "$BODY_HASH")"
  SIGNATURE="$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" | awk '{print $NF}')"
  URL="$BASE_URL$(printf '%s' "$PUBLIC_PATH" | sed 's#^/api##')"
  if [ -n "$QUERY" ]; then
    URL="$URL?$QUERY"
  fi

  if [ -n "$BODY" ]; then
    STATUS="$(printf '%s' "$BODY" | curl $CURL_OPTS -o "$OUT" -w '%{http_code}' \
      -X "$METHOD" \
      -H "Content-Type: application/json" \
      -H "X-BPMT-App-Key: $APP_KEY" \
      -H "X-BPMT-Timestamp: $TIMESTAMP" \
      -H "X-BPMT-Nonce: $NONCE" \
      -H "X-BPMT-Signature: $SIGNATURE" \
      --data-binary @- \
      "$URL")"
  else
    STATUS="$(curl $CURL_OPTS -o "$OUT" -w '%{http_code}' \
      -X "$METHOD" \
      -H "X-BPMT-App-Key: $APP_KEY" \
      -H "X-BPMT-Timestamp: $TIMESTAMP" \
      -H "X-BPMT-Nonce: $NONCE" \
      -H "X-BPMT-Signature: $SIGNATURE" \
      "$URL")"
  fi

  if [ "$STATUS" != "$EXPECTED_STATUS" ]; then
    printf '%s\n' "Expected $EXPECTED_STATUS for $METHOD $PUBLIC_PATH, got $STATUS"
    cat "$OUT"
    exit 1
  fi
}

signed_request GET /api/v1/dynamic-tables 'order=desc&sort=createDate' '' /tmp/bpmt-api-dynamic-tables.json 200

grep -q '"success":true' /tmp/bpmt-api-dynamic-tables.json
grep -q '"sort":"createDate"' /tmp/bpmt-api-dynamic-tables.json
grep -q '"order":"desc"' /tmp/bpmt-api-dynamic-tables.json

MISSING_BODY='{"name":"RV_API_MISSING","columns":[{"name":"ID","type":"String","totalSize":64,"primaryKey":true,"required":true}]}'
signed_request PUT /api/v1/dynamic-tables/RV_API_MISSING '' "$MISSING_BODY" /tmp/bpmt-api-missing-table.json 404
grep -q '"code":"DYNAMIC_TABLE_NOT_FOUND"' /tmp/bpmt-api-missing-table.json

printf '%s\n' "API smoke passed: $BASE_URL"
