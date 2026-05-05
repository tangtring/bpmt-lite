#!/bin/sh
set -eu

CERT_DIR="${BPMT_TLS_CERT_DIR:-certs}"
CERT_FILE="$CERT_DIR/fullchain.pem"
KEY_FILE="$CERT_DIR/privkey.pem"
TLS_DAYS="${BPMT_TLS_DAYS:-3650}"
TLS_HOSTS="${BPMT_TLS_HOSTS:-localhost}"
TLS_IPS="${BPMT_TLS_IPS:-127.0.0.1}"
TLS_FORCE="${BPMT_TLS_FORCE:-0}"

is_true() {
  case "$1" in
    1|true|TRUE|yes|YES|on|ON)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required to generate self-signed TLS certificates." >&2
  exit 1
fi

if [ -s "$CERT_FILE" ] && [ -s "$KEY_FILE" ] && ! is_true "$TLS_FORCE"; then
  echo "TLS certificate already exists: $CERT_FILE"
  echo "TLS private key already exists: $KEY_FILE"
  echo "Set BPMT_TLS_FORCE=1 to overwrite them."
  exit 0
fi

if ! is_true "$TLS_FORCE"; then
  if [ -e "$CERT_FILE" ] || [ -e "$KEY_FILE" ]; then
    echo "TLS certificate files are incomplete or already exist." >&2
    echo "Refusing to overwrite without BPMT_TLS_FORCE=1." >&2
    echo "Certificate: $CERT_FILE" >&2
    echo "Private key: $KEY_FILE" >&2
    exit 1
  fi
fi

case "$TLS_DAYS" in
  ''|*[!0-9]*)
    echo "BPMT_TLS_DAYS must be a positive integer." >&2
    exit 1
    ;;
  0)
    echo "BPMT_TLS_DAYS must be greater than 0." >&2
    exit 1
    ;;
esac

mkdir -p "$CERT_DIR"

tmp_conf="$(mktemp "${TMPDIR:-/tmp}/bpmt-openssl.XXXXXX")"
trap 'rm -f "$tmp_conf"' EXIT INT TERM

{
  echo "[req]"
  echo "distinguished_name = req_distinguished_name"
  echo "x509_extensions = v3_req"
  echo "prompt = no"
  echo
  echo "[req_distinguished_name]"
  echo "CN = localhost"
  echo
  echo "[v3_req]"
  echo "subjectAltName = @alt_names"
  echo
  echo "[alt_names]"

  alt_index=1
  old_ifs="$IFS"
  IFS=' ,'
  for host in $TLS_HOSTS; do
    if [ -n "$host" ]; then
      echo "DNS.$alt_index = $host"
      alt_index=$((alt_index + 1))
    fi
  done
  ip_index=1
  for ip in $TLS_IPS; do
    if [ -n "$ip" ]; then
      echo "IP.$ip_index = $ip"
      ip_index=$((ip_index + 1))
    fi
  done
  IFS="$old_ifs"
} >"$tmp_conf"

openssl req \
  -x509 \
  -nodes \
  -newkey rsa:2048 \
  -days "$TLS_DAYS" \
  -keyout "$KEY_FILE" \
  -out "$CERT_FILE" \
  -config "$tmp_conf" \
  -extensions v3_req \
  >/dev/null 2>&1

chmod 600 "$KEY_FILE"
chmod 644 "$CERT_FILE"

echo "Generated TLS certificate: $CERT_FILE"
echo "Generated TLS private key: $KEY_FILE"
