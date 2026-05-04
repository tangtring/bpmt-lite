# v1.5.4 multi-arch 发布修复

## 背景

`v1.5.3` 发布时 Web/API 镜像是在 Apple Silicon 本机路径下生成和同步的。经 `docker buildx imagetools inspect` 确认：

- `ghcr.io/wodenwang/bpmt-lite:1.5.3` 只包含 `linux/arm64`
- `ghcr.io/wodenwang/bpmt-lite-api:1.5.3` 只包含 `linux/arm64`

因此 x86_64 Linux 服务器使用默认 `docker compose` 拉取 `1.5.3` 时无法获得匹配架构镜像。

## 修复策略

`v1.5.4` 起，正式 GHCR 镜像发布必须使用 Docker buildx 生成 multi-arch manifest，同一个版本 tag 同时包含：

- `linux/amd64`
- `linux/arm64`

Java WAR 产物本身不区分 CPU 架构，不需要为 multi-arch 修改业务代码。

## 发布入口

正式发布使用：

```bash
scripts/build-multiarch-images.sh
```

该脚本默认：

- 使用 Java 8 和 `settings.local.xml` 构建 Web/API WAR
- 推送 `ghcr.io/wodenwang/bpmt-lite:<project.version>`
- 推送 `ghcr.io/wodenwang/bpmt-lite-api:<project.version>`
- 同步两个镜像的 `latest`
- 检查发布后的 manifest

本地单架构 smoke 仍可继续使用：

```bash
scripts/build-image.sh
scripts/build-api-image.sh
```

这两个脚本只验证当前 Docker daemon 架构，不作为正式发布入口。

## 发布前准备

```bash
docker login ghcr.io
docker buildx create --name bpmt-multi --use || docker buildx use bpmt-multi
docker buildx inspect --bootstrap
```

## 验收要求

发布后必须检查：

```bash
docker buildx imagetools inspect ghcr.io/wodenwang/bpmt-lite:<version>
docker buildx imagetools inspect ghcr.io/wodenwang/bpmt-lite-api:<version>
```

验收记录中必须包含：

- Web 镜像 manifest 有 `linux/amd64`
- Web 镜像 manifest 有 `linux/arm64`
- API 镜像 manifest 有 `linux/amd64`
- API 镜像 manifest 有 `linux/arm64`
- x86_64 Linux 服务器至少完成一次 `docker compose pull` 或 `docker compose up -d` smoke

## 临时 tag

如果需要验证候选镜像且不覆盖 `latest`：

```bash
BPMT_SYNC_LATEST=false scripts/build-multiarch-images.sh
```

也可以通过 `BPMT_IMAGE_TAG` 和 `BPMT_API_IMAGE_TAG` 指定临时 tag。
