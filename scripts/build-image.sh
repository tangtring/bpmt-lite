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

mvn -s settings.local.xml -pl platform -am -Pdocker-image verify

IMAGE_TAG="$(mvn -s settings.local.xml -q -pl platform help:evaluate -Dexpression=project.version -DforceStdout)"
IMAGE_NAME="ghcr.io/wodenwang/bpmt-lite"
IMAGE="${IMAGE_NAME}:${IMAGE_TAG}"

docker run --rm --entrypoint sh "$IMAGE" -lc '
  test -d /usr/local/tomcat/webapps/ROOT
  test -d /usr/local/tomcat/webapps/ueditor
  test -x /usr/local/bin/bpmt-entrypoint.sh
  fc-match "WenQuanYi Zen Hei" | grep -q "wqy"
  fc-list :lang=zh | grep -q .
'

printf '%s\n' "Docker image verified: $IMAGE"
