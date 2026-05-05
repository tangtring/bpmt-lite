#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1}"
CALLBACK_URL="${CALLBACK_URL:-http://127.0.0.1/demo/callback}"
CLIENT_ID="${CLIENT_ID:-wechat-smoke-client}"
STATE="${STATE:-wechat-smoke-state}"
DB_NAME="${DB_NAME:-bpmt}"
DB_PASSWORD="${DB_PASSWORD:-123456}"

if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is required." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 is required for URL encoding." >&2
  exit 1
fi

urlencode() {
  python3 - "$1" <<'PY'
import sys
from urllib.parse import quote
print(quote(sys.argv[1], safe=""))
PY
}

location_from_headers() {
  awk 'BEGIN{IGNORECASE=1} /^Location:/ {sub(/\r$/, "", $0); sub(/^[^:]*:[[:space:]]*/, "", $0); print; exit}' "$1"
}

mask_location() {
  sed -E 's/([?&](code|client_secret|access_token)=)[^&]*/\1***/g'
}

require_location_contains() {
  local location="$1"
  local expected="$2"
  local label="$3"
  if [[ "$location" != *"$expected"* ]]; then
    echo "ERROR: ${label} location mismatch." >&2
    echo "expected contains: ${expected}" >&2
    echo "actual: $(printf '%s' "$location" | mask_location)" >&2
    exit 1
  fi
}

tmp_override="$(mktemp "${TMPDIR:-/tmp}/bpmt-wechat-fake.XXXXXX.yml")"
headers_file="$(mktemp "${TMPDIR:-/tmp}/bpmt-wechat-headers.XXXXXX")"
cookie_jar="$(mktemp "${TMPDIR:-/tmp}/bpmt-wechat-cookie.XXXXXX")"
trap 'rm -f "$tmp_override" "$headers_file" "$cookie_jar"' EXIT

cat >"$tmp_override" <<'YAML'
services:
  bpmt-web:
    environment:
      BPMT_OAUTH_WECHAT_FAKE_PROVIDER: "true"
      BPMT_OAUTH_WECHAT_FAKE_CODE: "fake-admin"
YAML

echo "本脚本是本机 fake WeChat OAuth smoke，会使用临时 compose override recreate bpmt-web；不会删除 db/data 或运行目录。"
docker compose -f docker-compose.yml -f "$tmp_override" up -d --force-recreate bpmt-web

for i in $(seq 1 60); do
  if curl -fsS -o /dev/null "${BASE_URL%/}/"; then
    break
  fi
  if [[ "$i" == "60" ]]; then
    echo "ERROR: bpmt-web is not ready at ${BASE_URL%/}/." >&2
    exit 1
  fi
  sleep 2
done

docker compose exec -T bpmt-mariadb mariadb -uroot -p"${DB_PASSWORD}" "${DB_NAME}" <<SQL
ALTER TABLE CM_THIRDPART ADD COLUMN IF NOT EXISTS WECHAT_LOGIN_ENABLED tinyint NOT NULL DEFAULT 0;
ALTER TABLE CM_THIRDPART ADD COLUMN IF NOT EXISTS WECHAT_TYPE varchar(20) DEFAULT NULL;
ALTER TABLE CM_THIRDPART ADD COLUMN IF NOT EXISTS WECHAT_KEY varchar(100) DEFAULT NULL;
ALTER TABLE CM_THIRDPART ADD COLUMN IF NOT EXISTS WECHAT_SCOPE varchar(50) DEFAULT NULL;
INSERT INTO CM_THIRDPART (
  THIRDPART_KEY,
  THIRDPART_NAME,
  CLIENT_ID,
  CLIENT_SECRET_HASH,
  REDIRECT_URIS,
  HOME_URL,
  WECHAT_LOGIN_ENABLED,
  WECHAT_TYPE,
  WECHAT_KEY,
  WECHAT_SCOPE,
  PRI_KEY,
  ACTIVE_FLAG,
  DESCRIPTION,
  CREATE_TIME,
  UPDATE_TIME
) VALUES (
  'wechat-smoke',
  '微信 smoke',
  '${CLIENT_ID}',
  SHA2('wechat-smoke-secret', 256),
  '${CALLBACK_URL}',
  '${CALLBACK_URL}',
  1,
  'agent',
  'fake-agent',
  NULL,
  'sys_thirdpart',
  1,
  'local fake WeChat OAuth smoke',
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  THIRDPART_NAME=VALUES(THIRDPART_NAME),
  CLIENT_SECRET_HASH=VALUES(CLIENT_SECRET_HASH),
  REDIRECT_URIS=VALUES(REDIRECT_URIS),
  HOME_URL=VALUES(HOME_URL),
  WECHAT_LOGIN_ENABLED=VALUES(WECHAT_LOGIN_ENABLED),
  WECHAT_TYPE=VALUES(WECHAT_TYPE),
  WECHAT_KEY=VALUES(WECHAT_KEY),
  WECHAT_SCOPE=VALUES(WECHAT_SCOPE),
  PRI_KEY=VALUES(PRI_KEY),
  ACTIVE_FLAG=VALUES(ACTIVE_FLAG),
  DESCRIPTION=VALUES(DESCRIPTION),
  UPDATE_TIME=NOW();
SQL

encoded_callback="$(urlencode "$CALLBACK_URL")"
encoded_state="$(urlencode "$STATE")"
authorize_url="${BASE_URL%/}/oauth/authorize?response_type=code&client_id=${CLIENT_ID}&redirect_uri=${encoded_callback}&state=${encoded_state}"

curl -sS -D "$headers_file" -o /dev/null -c "$cookie_jar" -b "$cookie_jar" \
  -H "User-Agent: Mozilla/5.0" \
  "$authorize_url"
normal_location="$(location_from_headers "$headers_file")"
require_location_contains "$normal_location" "/login.jsp" "non-WeChat authorize"
echo "PASS non-WeChat authorize falls back to BPMT login."

curl -sS -D "$headers_file" -o /dev/null -c "$cookie_jar" -b "$cookie_jar" \
  -H "User-Agent: Mozilla/5.0 MicroMessenger" \
  "$authorize_url"
fake_location="$(location_from_headers "$headers_file")"
require_location_contains "$fake_location" "/oauth/authorize" "WeChat fake redirect"
require_location_contains "$fake_location" "code=fake-admin" "WeChat fake redirect"
echo "PASS WeChat authorize redirects to fake callback."

curl -sS -D "$headers_file" -o /dev/null -c "$cookie_jar" -b "$cookie_jar" \
  -H "User-Agent: Mozilla/5.0 MicroMessenger" \
  "$fake_location"
final_location="$(location_from_headers "$headers_file")"
require_location_contains "$final_location" "${CALLBACK_URL}" "OAuth final callback"
require_location_contains "$final_location" "state=${STATE}" "OAuth final callback"
require_location_contains "$final_location" "code=" "OAuth final callback"
echo "PASS fake callback creates OAuth authorization code and returns to third-party callback."
echo "DONE WeChat OAuth fake smoke passed."
