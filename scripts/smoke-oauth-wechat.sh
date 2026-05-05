#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1}"
CALLBACK_URL="${CALLBACK_URL:-http://127.0.0.1/demo/callback}"
CLIENT_ID="${CLIENT_ID:-wechat-smoke-client}"
STATE="${STATE:-wechat-smoke-state}"
DB_NAME="${DB_NAME:-bpmt}"
DB_PASSWORD="${DB_PASSWORD:-123456}"

SMOKE_THIRDPART_KEY="wechat-smoke"
tmp_override=""
headers_file=""
cookie_jar=""
db_cleanup_enabled=0

if [[ "$CLIENT_ID" != wechat-smoke-* ]]; then
  echo "ERROR: CLIENT_ID must start with 'wechat-smoke-' for this local smoke." >&2
  exit 1
fi

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

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

location_from_headers() {
  awk 'BEGIN{IGNORECASE=1} /^Location:/ {sub(/\r$/, "", $0); sub(/^[^:]*:[[:space:]]*/, "", $0); print; exit}' "$1"
}

mask_location() {
  sed -E 's/([?&](code|client_secret|access_token)=)[^&]*/\1***/g'
}

masked() {
  printf "%s" "$1" | mask_location
}

require_location_contains() {
  local location="$1"
  local expected="$2"
  local label="$3"
  if [[ "$location" != *"$expected"* ]]; then
    echo "ERROR: ${label} location mismatch." >&2
    echo "expected contains: $(masked "$expected")" >&2
    echo "actual: $(masked "$location")" >&2
    exit 1
  fi
}

mariadb_exec() {
  docker compose exec -T bpmt-mariadb mariadb -uroot -p"${DB_PASSWORD}" --database="${DB_NAME}" "$@"
}

cleanup_smoke_records() {
  mariadb_exec <<SQL
DELETE FROM CM_THIRDPART_ACCESS_TOKEN WHERE THIRDPART_KEY='${SMOKE_THIRDPART_KEY}';
DELETE FROM CM_THIRDPART_AUTH_CODE WHERE THIRDPART_KEY='${SMOKE_THIRDPART_KEY}';
DELETE FROM CM_THIRDPART WHERE THIRDPART_KEY='${SMOKE_THIRDPART_KEY}';
SQL
}

verify_fake_provider_removed() {
  local env_text
  env_text="$(docker inspect bpmt-web --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null)"
  if printf "%s\n" "$env_text" | grep -q '^BPMT_OAUTH_WECHAT_FAKE_PROVIDER=true$'; then
    echo "WARN: bpmt-web still contains BPMT_OAUTH_WECHAT_FAKE_PROVIDER=true after restore." >&2
    return 1
  fi
  return 0
}

cleanup() {
  local status=$?
  trap - EXIT
  set +e

  if [[ "$db_cleanup_enabled" == "1" ]]; then
    cleanup_smoke_records >/dev/null 2>&1
  fi

  rm -f "$tmp_override" "$headers_file" "$cookie_jar"

  echo "恢复 bpmt-web/bpmt-nginx 到正常 compose 环境..."
  if ! docker compose up -d --force-recreate bpmt-web bpmt-nginx >/dev/null 2>&1; then
    echo "WARN: failed to restore bpmt-web/bpmt-nginx with normal compose." >&2
  fi
  verify_fake_provider_removed || true

  exit "$status"
}

wait_for_http() {
  local url="$1"
  local label="$2"
  local i
  for i in $(seq 1 60); do
    if curl -fsS -o /dev/null "$url"; then
      return 0
    fi
    sleep 2
  done
  echo "ERROR: ${label} is not ready at ${url}." >&2
  return 1
}

ensure_wechat_columns() {
  local db_name_sql
  local missing_count
  db_name_sql="$(sql_escape "$DB_NAME")"
  missing_count="$(mariadb_exec -N -B <<SQL
SELECT 4 - COUNT(*)
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA='${db_name_sql}'
  AND TABLE_NAME='CM_THIRDPART'
  AND COLUMN_NAME IN ('WECHAT_LOGIN_ENABLED','WECHAT_TYPE','WECHAT_KEY','WECHAT_SCOPE');
SQL
)"
  if [[ "$missing_count" != "0" ]]; then
    mariadb_exec < database/v1.6.1-wechat-oauth-thirdpart.sql
  fi
}

tmp_override="$(mktemp "${TMPDIR:-/tmp}/bpmt-wechat-fake.XXXXXX.yml")"
headers_file="$(mktemp "${TMPDIR:-/tmp}/bpmt-wechat-headers.XXXXXX")"
cookie_jar="$(mktemp "${TMPDIR:-/tmp}/bpmt-wechat-cookie.XXXXXX")"
trap cleanup EXIT

cat >"$tmp_override" <<'YAML'
services:
  bpmt-web:
    environment:
      BPMT_OAUTH_WECHAT_FAKE_PROVIDER: "true"
      BPMT_OAUTH_WECHAT_FAKE_CODE: "fake-admin"
YAML

echo "本脚本是本机 fake WeChat OAuth smoke，会临时 recreate bpmt-web/bpmt-nginx；退出时会恢复真实 provider 并清理 smoke 记录。"
docker compose -f docker-compose.yml -f "$tmp_override" up -d --force-recreate bpmt-web bpmt-nginx
wait_for_http "${BASE_URL%/}/" "bpmt-nginx"

ensure_wechat_columns
db_cleanup_enabled=1
cleanup_smoke_records

client_id_sql="$(sql_escape "$CLIENT_ID")"
callback_url_sql="$(sql_escape "$CALLBACK_URL")"

mariadb_exec <<SQL
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
  '${SMOKE_THIRDPART_KEY}',
  '微信 smoke',
  '${client_id_sql}',
  SHA2('wechat-smoke-secret', 256),
  '${callback_url_sql}',
  '${callback_url_sql}',
  1,
  'agent',
  'fake-agent',
  NULL,
  'sys_thirdpart',
  1,
  'local fake WeChat OAuth smoke',
  NOW(),
  NOW()
);
SQL

encoded_client_id="$(urlencode "$CLIENT_ID")"
encoded_callback="$(urlencode "$CALLBACK_URL")"
encoded_state="$(urlencode "$STATE")"
authorize_url="${BASE_URL%/}/oauth/authorize?response_type=code&client_id=${encoded_client_id}&redirect_uri=${encoded_callback}&state=${encoded_state}"

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
