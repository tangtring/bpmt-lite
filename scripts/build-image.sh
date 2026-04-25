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
