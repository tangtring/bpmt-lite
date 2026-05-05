#!/bin/sh
set -eu

HTTPS_ENABLED="${BPMT_HTTPS_ENABLED:-0}"
HTTP_REDIRECT="${BPMT_HTTP_REDIRECT:-true}"
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

  cat <<NGINX
server {
    $listen_line;
    server_name _;

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
      cat <<'NGINX'
server {
    listen 80;
    server_name _;

    return 301 https://$http_host$request_uri;
}

NGINX
    else
      write_proxy_server "listen 80" "http" "80"
    fi

    write_proxy_server "listen 443 ssl" "https" "443" | \
      sed "s|listen 443 ssl;|listen 443 ssl;\\
    ssl_certificate $TLS_CERT_FILE;\\
    ssl_certificate_key $TLS_KEY_FILE;|"
  else
    write_proxy_server "listen 80" "http" "80"
  fi
} >"$OUTPUT_FILE"

echo "Generated nginx config: $OUTPUT_FILE"
