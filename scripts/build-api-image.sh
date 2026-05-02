#!/bin/sh
set -eu

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

mvn -s settings.local.xml -pl api -am -Pdocker-image package

IMAGE_TAG="$(mvn -s settings.local.xml -q -pl api help:evaluate -Dexpression=project.version -DforceStdout)"
IMAGE_NAME="${BPMT_API_IMAGE_NAME:-ghcr.io/wodenwang/bpmt-lite-api}"
IMAGE="${IMAGE_NAME}:${BPMT_API_IMAGE_TAG:-$IMAGE_TAG}"

BUILD_DIR="api/target/docker"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
cp api/target/api.war "$BUILD_DIR/api.war"
cp docker/Dockerfile.api "$BUILD_DIR/Dockerfile"
cp docker/docker-entrypoint.sh "$BUILD_DIR/docker-entrypoint.sh"

docker build -t "$IMAGE" "$BUILD_DIR"

docker run --rm --entrypoint sh "$IMAGE" -lc '
  test -d /usr/local/tomcat/webapps/api
  test -f /usr/local/tomcat/webapps/api/openapi.json
  test -f /usr/local/tomcat/webapps/api/docs/index.html
  test -x /usr/local/bin/bpmt-entrypoint.sh
'

printf '%s\n' "Docker image verified: $IMAGE"
