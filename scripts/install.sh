#!/bin/sh
set -eu

MODE="${1:-min}"
REF="${BPMT_REF:-v1.5.3}"
INSTALL_DIR="${BPMT_HOME:-bpmt-lite}"
RAW_BASE_URL="${BPMT_RAW_BASE_URL:-https://raw.githubusercontent.com/wodenwang/bpmt-lite/$REF}"
SQL_BASE_URL="${BPMT_SQL_BASE_URL:-$RAW_BASE_URL/database}"

usage() {
  echo "Usage: install.sh [min|full]" >&2
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
  min)
    run_arg="min"
    ;;
  full|"")
    run_arg=""
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

mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

download "$RAW_BASE_URL/scripts/run.sh" run.sh
chmod +x run.sh

BPMT_RAW_BASE_URL="$RAW_BASE_URL" BPMT_SQL_BASE_URL="$SQL_BASE_URL" sh ./run.sh $run_arg
