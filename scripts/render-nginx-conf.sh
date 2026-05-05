#!/bin/sh
set -eu

HTTPS_ENABLED="${BPMT_HTTPS_ENABLED:-0}"
HTTP_REDIRECT="${BPMT_HTTP_REDIRECT:-true}"
HTTP_PORT="${BPMT_HTTP_PORT:-80}"
HTTPS_PORT="${BPMT_HTTPS_PORT:-443}"
TLS_CERT_FILE="${BPMT_TLS_CERT_FILE:-/etc/nginx/certs/fullchain.pem}"
TLS_KEY_FILE="${BPMT_TLS_KEY_FILE:-/etc/nginx/certs/privkey.pem}"
OUTPUT_FILE="${BPMT_NGINX_CONF:-docker/nginx/nginx.conf}"

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

mkdir -p "$(dirname "$OUTPUT_FILE")"

https_redirect_target() {
  if [ "$HTTPS_PORT" = "443" ]; then
    echo 'https://$host$request_uri'
  else
    echo "https://\$host:$HTTPS_PORT\$request_uri"
  fi
}

write_proxy_locations() {
  proto="$1"
  port="$2"

  cat <<NGINX
    location /api/ {
        proxy_pass http://bpmt-api:8081/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$http_host;
        proxy_set_header X-Forwarded-Host \$http_host;
        proxy_set_header X-Forwarded-Proto $proto;
        proxy_set_header X-Forwarded-Port $port;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    location /ueditor/ {
        proxy_pass http://bpmt-web:8080/ueditor/;
        proxy_http_version 1.1;
        proxy_set_header Host \$http_host;
        proxy_set_header X-Forwarded-Host \$http_host;
        proxy_set_header X-Forwarded-Proto $proto;
        proxy_set_header X-Forwarded-Port $port;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    location / {
        proxy_pass http://bpmt-web:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host \$http_host;
        proxy_set_header X-Forwarded-Host \$http_host;
        proxy_set_header X-Forwarded-Proto $proto;
        proxy_set_header X-Forwarded-Port $port;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP \$remote_addr;
    }
NGINX
}

write_proxy_server() {
  listen_line="$1"
  proto="$2"
  port="$3"
  cert_file="${4:-}"
  key_file="${5:-}"

  cat <<NGINX
server {
    $listen_line;
    server_name _;

NGINX
  if [ -n "$cert_file" ]; then
    cat <<NGINX
    ssl_certificate $cert_file;
    ssl_certificate_key $key_file;

NGINX
  fi
  cat <<NGINX
    client_max_body_size 100m;

NGINX
  write_proxy_locations "$proto" "$port"
  cat <<'NGINX'
}
NGINX
}

{
  if is_true "$HTTPS_ENABLED"; then
    if is_true "$HTTP_REDIRECT"; then
      redirect_target="$(https_redirect_target)"
      cat <<NGINX
server {
    listen 80;
    server_name _;

    return 301 $redirect_target;
}

NGINX
    else
      write_proxy_server "listen 80" "http" "$HTTP_PORT"
    fi

    write_proxy_server "listen 443 ssl" "https" "$HTTPS_PORT" "$TLS_CERT_FILE" "$TLS_KEY_FILE"
  else
    write_proxy_server "listen 80" "http" "$HTTP_PORT"
  fi
} >"$OUTPUT_FILE"

echo "Generated nginx config: $OUTPUT_FILE"
