#!/bin/sh
set -eu

TARGET_REF="${1:-${BPMT_TARGET_REF:-}}"
STATE_DIR="${BPMT_STATE_DIR:-.bpmt-lite}"
VERSION_FILE="$STATE_DIR/version"
RAW_BASE_URL="${BPMT_RAW_BASE_URL:-}"
GITHUB_API_URL="${BPMT_GITHUB_API_URL:-https://api.github.com/repos/wodenwang/bpmt-lite/releases/latest}"
WEB_IMAGE="${BPMT_WEB_IMAGE:-ghcr.io/wodenwang/bpmt-lite:latest}"
API_IMAGE="${BPMT_API_IMAGE:-ghcr.io/wodenwang/bpmt-lite-api:latest}"

usage() {
  echo "Usage: upgrade.sh [target-ref]" >&2
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 is required." >&2
    exit 1
  fi
}

download() {
  url="$1"
  target="$2"
  tmp="$target.tmp"
  curl -fsSL "$url" -o "$tmp"
  mv "$tmp" "$target"
}

download_optional() {
  url="$1"
  target="$2"
  tmp="$target.tmp"
  if curl -fsSL "$url" -o "$tmp" 2>/dev/null; then
    mv "$tmp" "$target"
    return 0
  fi
  rm -f "$tmp"
  return 1
}

latest_release_ref() {
  tmp="$STATE_DIR/latest-release.json.tmp"
  curl -fsSL "$GITHUB_API_URL" -o "$tmp"
  sed -n 's/.*"tag_name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$tmp" | head -n 1
  rm -f "$tmp"
}

current_version() {
  if [ -f "$VERSION_FILE" ]; then
    sed -n 's/^version=//p' "$VERSION_FILE" | tail -n 1
  fi
}

set_env_value() {
  key="$1"
  value="$2"
  env_file=".env"
  tmp="$env_file.tmp"

  touch "$env_file"
  if grep -q "^$key=" "$env_file"; then
    sed "s|^$key=.*|$key=$value|" "$env_file" > "$tmp"
    mv "$tmp" "$env_file"
  else
    printf '%s=%s\n' "$key" "$value" >> "$env_file"
  fi
}

apply_sql_file() {
  sql_file="$1"
  db_name="${DB_NAME:-bpmt}"
  db_user="${DB_USER:-root}"
  db_password="${DB_PASSWORD:-123456}"

  echo "Applying SQL upgrade: $sql_file"
  docker compose exec -T bpmt-mariadb mariadb -u"$db_user" -p"$db_password" "$db_name" < "$sql_file"
}

apply_upgrade_sqls() {
  from_ref="$1"
  to_ref="$2"
  manifest="$STATE_DIR/upgrade-manifest.txt"
  current="$from_ref"

  if [ -z "$from_ref" ] || [ "$from_ref" = "$to_ref" ]; then
    echo "No SQL upgrade needed."
    return 0
  fi

  if ! download_optional "$RAW_BASE_URL/database/upgrade/manifest.txt" "$manifest"; then
    echo "No SQL upgrade manifest found for $to_ref."
    return 0
  fi

  while [ "$current" != "$to_ref" ]; do
    line=$(grep "^$current|" "$manifest" | head -n 1 || true)
    if [ -z "$line" ]; then
      echo "No SQL upgrade step from $current to $to_ref."
      return 0
    fi
    next=$(printf '%s\n' "$line" | awk -F'|' '{print $2}')
    sql_path=$(printf '%s\n' "$line" | awk -F'|' '{print $3}')
    if [ -z "$next" ] || [ -z "$sql_path" ]; then
      echo "Invalid SQL upgrade manifest line: $line" >&2
      exit 1
    fi
    sql_file="$STATE_DIR/$(basename "$sql_path")"
    download "$RAW_BASE_URL/$sql_path" "$sql_file"
    apply_sql_file "$sql_file"
    current="$next"
  done
}

case "${TARGET_REF:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
esac

need_cmd curl
need_cmd docker
need_cmd awk
need_cmd sed

mkdir -p "$STATE_DIR"

if [ -z "$TARGET_REF" ]; then
  TARGET_REF="$(latest_release_ref)"
fi

if [ -z "$TARGET_REF" ]; then
  echo "Cannot resolve latest bpmt-lite release." >&2
  exit 1
fi

if [ -z "$RAW_BASE_URL" ]; then
  RAW_BASE_URL="https://raw.githubusercontent.com/wodenwang/bpmt-lite/$TARGET_REF"
fi

previous_version="$(current_version || true)"
compose_ref_file="docker-compose-$TARGET_REF.yml"

download "$RAW_BASE_URL/docker-compose.yml" "$compose_ref_file"
echo "Downloaded reference compose: $compose_ref_file"

apply_upgrade_sqls "$previous_version" "$TARGET_REF"

docker pull "$WEB_IMAGE"
docker pull "$API_IMAGE"

if [ -f .env ]; then
  cp .env "$STATE_DIR/env.$(date -u '+%Y%m%d%H%M%S').bak"
fi
set_env_value BPMT_IMAGE_TAG latest
set_env_value BPMT_API_IMAGE_TAG latest

docker compose --env-file .env up -d --no-deps bpmt-web bpmt-api

{
  echo "version=$TARGET_REF"
  echo "previous_version=${previous_version:-unknown}"
  echo "image_tag=latest"
  echo "api_image_tag=latest"
  echo "upgraded_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
} > "$VERSION_FILE"

{
  echo "$(date -u '+%Y-%m-%dT%H:%M:%SZ') ${previous_version:-unknown} -> $TARGET_REF"
} >> "$STATE_DIR/upgrade.log"

echo "bpmt-lite upgraded to $TARGET_REF with latest Web/API images."
