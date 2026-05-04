# v1.5.4 发布记录

## 发布定位

`v1.5.4` 是基于 `v1.5.3` 的 multi-arch 发布补丁。业务行为保持 `v1.5.3` 基线：非 80 端口 OAuth 回跳地址保留端口，OAuth 登录态切换体验继续保留，工作流待办“查看/处理”跳转修复继续保留。

本版本重点修复 Web/API 镜像正式 tag 只包含 `linux/arm64` 的发布问题，让 x86_64 Linux 服务器可以直接使用默认 `docker compose` 拉取并运行 `1.5.4`。

## 变更内容

- Maven 项目版本切到 `1.5.4`。
- 默认 Web/API 镜像 tag 切到 `1.5.4`。
- `scripts/install.sh`、`scripts/run.sh`、`scripts/init-db.sh` 默认 release/raw tag 切到 `v1.5.4`。
- `api/src/main/webapp/openapi.json` 的 `info.title` 和 `info.version` 切到 `v1.5.4`。
- Web/API Dockerfile 按 CPU 架构选择 Ubuntu apt 镜像源：
  - `linux/amd64` 使用 `https://mirrors.aliyun.com/ubuntu`
  - `linux/arm64` 使用 `https://mirrors.aliyun.com/ubuntu-ports`
- 正式发布入口使用 `scripts/build-multiarch-images.sh` 推送 Web/API multi-arch 镜像并同步 `latest`。

## 验收摘要

2026-05-05 已完成发布验收：

- `scripts/verify-repo.sh` 通过。
- `docker compose config` 通过，默认镜像 tag 为 `1.5.4`。
- Java 8 全仓编译通过：`mvn -s settings.local.xml -DskipTests compile`。
- API 定向单测通过：39 项，FAIL 0，ERROR 0。
- 本地 Web 镜像 smoke 通过：`scripts/build-image.sh`。
- 本地 API 镜像 smoke 通过：`scripts/build-api-image.sh`。
- multi-arch 发布通过：`scripts/build-multiarch-images.sh`。
- 强制 `linux/amd64` 拉取 Web/API `1.5.4` 镜像通过。
- 临时 compose 项目 `bpmt-v154-smoke` 使用最小库 `bpmt_min` 验证通过：
  - `/` 返回 200
  - `/ueditor/` 返回 200
  - `/api/docs/` 返回 200
  - `/api/openapi.json` 返回 200
  - `scripts/smoke-api.sh` 通过
  - `bpmt_min` 初始化后 176 张表
  - Web/API Hazelcast 日志均出现 `Members [2]`

补充说明：直接执行 `mvn -s settings.local.xml -pl api -am test` 会先运行上游 `dbtools` 历史测试，并因缺少 `src/test/resources/database/*.h2.db.bak` 失败，未进入 API 模块；本次 API 验收使用项目既有的 API 定向测试命令。

## 发布产物

- Web image：`ghcr.io/wodenwang/bpmt-lite:1.5.4`
- Web manifest digest：`sha256:41efc7c12a72ea7d01c175602562bcfc99330f99dd8137f81101a5311048466b`
- Web amd64 digest：`sha256:4d78f0c40d7c9f3812461085b2e118630ab9167013a6aa089cdd1e5e655efe66`
- Web arm64 digest：`sha256:b36edd2a9273c2dbde75aa52f350fd06c4ccb9f31240af48095e3bb9851d7631`
- API image：`ghcr.io/wodenwang/bpmt-lite-api:1.5.4`
- API manifest digest：`sha256:6e8ee82982e74270755790202c9237f7dc70c2c002e8df1a5eacdfef4fcabd78`
- API amd64 digest：`sha256:b02a55ad350d3f15bd9f336969192f12847458d1245307763f1546bb4e7d84b8`
- API arm64 digest：`sha256:b0adac8a662c23f9de599692c01dc098686c23eaf7f56934679a983eec0a9343`
- `latest` 已同步到上述 Web/API manifest digest。
