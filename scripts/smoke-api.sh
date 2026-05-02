#!/bin/sh
set -eu

BASE_URL="${BPMT_API_BASE_URL:-http://127.0.0.1:8081/api}"
APP_KEY="${BPMT_API_APP_KEY:-bpmt-api}"
APP_SECRET="${BPMT_API_APP_SECRET:-bpmt-api-secret}"

curl -fsSI "$BASE_URL/openapi.json" | grep -q '200'
curl -fsSI "$BASE_URL/docs/" | grep -q '200'

STATUS="$(curl -s -o /tmp/bpmt-api-unauthorized.json -w '%{http_code}' "$BASE_URL/v1/dynamic-tables")"
if [ "$STATUS" != "401" ]; then
  printf '%s\n' "Expected 401 without signature, got $STATUS"
  cat /tmp/bpmt-api-unauthorized.json
  exit 1
fi

TIMESTAMP="$(date +%s)"
NONCE="smoke-$$"
BODY_HASH="$(printf '' | shasum -a 256 | awk '{print $1}')"
CANONICAL="$(printf 'GET\n/api/v1/dynamic-tables\n\n%s\n%s\n%s' "$TIMESTAMP" "$NONCE" "$BODY_HASH")"
SIGNATURE="$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" | awk '{print $2}')"

curl -fsS \
  -H "X-BPMT-App-Key: $APP_KEY" \
  -H "X-BPMT-Timestamp: $TIMESTAMP" \
  -H "X-BPMT-Nonce: $NONCE" \
  -H "X-BPMT-Signature: $SIGNATURE" \
  "$BASE_URL/v1/dynamic-tables" >/tmp/bpmt-api-dynamic-tables.json

grep -q '"success":true' /tmp/bpmt-api-dynamic-tables.json

printf '%s\n' "API smoke passed: $BASE_URL"
