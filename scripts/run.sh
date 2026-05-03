#!/bin/sh
set -eu

MODE="${1:-full}"
REF="${BPMT_REF:-v1.5.1}"
RAW_BASE_URL="${BPMT_RAW_BASE_URL:-https://raw.githubusercontent.com/wodenwang/bpmt-lite/$REF}"
SQL_BASE_URL="${BPMT_SQL_BASE_URL:-$RAW_BASE_URL/database}"

usage() {
  echo "Usage: scripts/run.sh [min]" >&2
}

download() {
  url="$1"
  target="$2"
  tmp="$target.tmp"

  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required." >&2
    exit 1
  fi

  curl -fsSL "$url" -o "$tmp"
  mv "$tmp" "$target"
}

case "$MODE" in
  full|"")
    db_name="bpmt"
    init_arg=""
    ;;
  min)
    db_name="bpmt_min"
    init_arg="min"
    ;;
  -h|--help|help)
    usage
    exit 0
    ;;
  *)
    usage
    exit 2
    ;;
esac

mkdir -p db/init docker/nginx

download "$RAW_BASE_URL/docker-compose.yml" docker-compose.yml
download "$RAW_BASE_URL/scripts/init-db.sh" init-db.sh
if [ -d docker/nginx/nginx.conf ]; then
  rm -rf docker/nginx/nginx.conf
fi
download "$RAW_BASE_URL/docker/nginx/nginx.conf" docker/nginx/nginx.conf

chmod +x init-db.sh
BPMT_SQL_BASE_URL="$SQL_BASE_URL" sh ./init-db.sh $init_arg

if [ "${BPMT_SKIP_UP:-}" = "1" ] || [ "${BPMT_SKIP_UP:-}" = "true" ]; then
  echo "Prepared docker-compose.yml and database SQL for $db_name."
  exit 0
fi

DB_NAME="$db_name" docker compose up -d

echo "bpmt-lite is starting with database: $db_name"
echo "URL: http://127.0.0.1:${BPMT_HTTP_PORT:-80}/"
echo "Login: admin/admin"
