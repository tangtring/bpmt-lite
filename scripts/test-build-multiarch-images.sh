#!/bin/sh
set -eu

fail() {
  printf '%s\n' "ERROR: $1" >&2
  exit 1
}

script="scripts/build-multiarch-images.sh"

test -f "$script" || fail "missing $script"
test -x "$script" || fail "$script is not executable"

content="$(cat "$script")"

printf '%s' "$content" | grep -q 'linux/amd64,linux/arm64' || fail "default platforms must include linux/amd64 and linux/arm64"
printf '%s' "$content" | grep -q 'docker buildx build' || fail "script must use docker buildx build"
printf '%s' "$content" | grep -q -- '--push' || fail "multi-arch release images must be pushed, not only loaded locally"
printf '%s' "$content" | grep -q 'ghcr.io/wodenwang/bpmt-lite' || fail "script must build the Web image"
printf '%s' "$content" | grep -q 'ghcr.io/wodenwang/bpmt-lite-api' || fail "script must build the API image"
printf '%s' "$content" | grep -q 'docker buildx imagetools inspect' || fail "script must inspect pushed manifests"

printf '%s\n' "OK: multi-arch image build script checks passed"
