#!/bin/sh
set -eu

fail() {
  printf '%s\n' "ERROR: $1" >&2
  exit 1
}

run() {
  printf '+ %s\n' "$*"
  "$@"
}

if [ ! -f settings.local.xml ]; then
  cp settings.example.xml settings.local.xml
  printf '%s\n' '已创建 settings.local.xml，请确认 Maven 私有依赖仓库路径后重新运行。'
  exit 1
fi

JAVA_VERSION_OUTPUT="$(java -version 2>&1 | head -n 1)"
case "$JAVA_VERSION_OUTPUT" in
  *'"1.8.'*) ;;
  *)
    printf '%s\n' "当前 Java 版本不是 Java 8：$JAVA_VERSION_OUTPUT"
    printf '%s\n' '请切换到 Java 8 后重新运行。'
    exit 1
    ;;
esac

command -v docker >/dev/null 2>&1 || fail "missing docker"
docker buildx version >/dev/null 2>&1 || fail "docker buildx is not available"

PLATFORMS="${BPMT_IMAGE_PLATFORMS:-linux/amd64,linux/arm64}"
WEB_IMAGE_NAME="${BPMT_IMAGE_NAME:-ghcr.io/wodenwang/bpmt-lite}"
API_IMAGE_NAME="${BPMT_API_IMAGE_NAME:-ghcr.io/wodenwang/bpmt-lite-api}"
SYNC_LATEST="${BPMT_SYNC_LATEST:-true}"
APT_MIRROR="${BPMT_DOCKER_APT_MIRROR:-https://mirrors.aliyun.com/ubuntu}"
APT_PORTS_MIRROR="${BPMT_DOCKER_APT_PORTS_MIRROR:-https://mirrors.aliyun.com/ubuntu-ports}"

PROJECT_VERSION="$(mvn -s settings.local.xml -q -pl platform help:evaluate -Dexpression=project.version -DforceStdout)"
WEB_FINAL_NAME="$(mvn -s settings.local.xml -q -pl platform help:evaluate -Dexpression=project.build.finalName -DforceStdout)"
API_FINAL_NAME="$(mvn -s settings.local.xml -q -pl api help:evaluate -Dexpression=project.build.finalName -DforceStdout)"

WEB_TAG="${BPMT_IMAGE_TAG:-$PROJECT_VERSION}"
API_TAG="${BPMT_API_IMAGE_TAG:-$PROJECT_VERSION}"
WEB_IMAGE="${WEB_IMAGE_NAME}:${WEB_TAG}"
API_IMAGE="${API_IMAGE_NAME}:${API_TAG}"

printf '%s\n' "Multi-arch platforms: $PLATFORMS"
printf '%s\n' "Web image: $WEB_IMAGE"
printf '%s\n' "API image: $API_IMAGE"

run mvn -s settings.local.xml -pl platform -am -Pdocker-image package
run cp "platform/target/${WEB_FINAL_NAME}.war" platform/target/docker/platform.war

WEB_TAG_ARGS="-t $WEB_IMAGE"
if [ "$SYNC_LATEST" = "true" ]; then
  WEB_TAG_ARGS="$WEB_TAG_ARGS -t ${WEB_IMAGE_NAME}:latest"
fi

# shellcheck disable=SC2086
run docker buildx build \
  --platform "$PLATFORMS" \
  --build-arg "APT_MIRROR=$APT_MIRROR" \
  --build-arg "APT_PORTS_MIRROR=$APT_PORTS_MIRROR" \
  $WEB_TAG_ARGS \
  --push \
  platform/target/docker

run mvn -s settings.local.xml -pl api -am -Pdocker-image package
run rm -rf api/target/docker
run mkdir -p api/target/docker
run cp "api/target/${API_FINAL_NAME}.war" api/target/docker/api.war
run cp docker/Dockerfile.api api/target/docker/Dockerfile
run cp docker/docker-entrypoint.sh api/target/docker/docker-entrypoint.sh

API_TAG_ARGS="-t $API_IMAGE"
if [ "$SYNC_LATEST" = "true" ]; then
  API_TAG_ARGS="$API_TAG_ARGS -t ${API_IMAGE_NAME}:latest"
fi

# shellcheck disable=SC2086
run docker buildx build \
  --platform "$PLATFORMS" \
  --build-arg "APT_MIRROR=$APT_MIRROR" \
  --build-arg "APT_PORTS_MIRROR=$APT_PORTS_MIRROR" \
  $API_TAG_ARGS \
  --push \
  api/target/docker

run docker buildx imagetools inspect "$WEB_IMAGE"
run docker buildx imagetools inspect "$API_IMAGE"

printf '%s\n' "Docker multi-arch images pushed and inspected:"
printf '%s\n' "  $WEB_IMAGE"
printf '%s\n' "  $API_IMAGE"
if [ "$SYNC_LATEST" = "true" ]; then
  printf '%s\n' "  ${WEB_IMAGE_NAME}:latest"
  printf '%s\n' "  ${API_IMAGE_NAME}:latest"
fi
