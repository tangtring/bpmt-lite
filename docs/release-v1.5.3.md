# v1.5.3 发布记录

## 发布定位

`v1.5.3` 是基于 `v1.5.2` 的发布阻塞修复补丁。`v1.5.2` 的 OAuth 登录态切换能力继续保留，`v1.5.1` 的工作流待办跳转修复继续保留。

## 变更内容

- 修复 `docker/nginx/nginx.conf` 代理转发的 `Host` 头，从 `$host` 调整为 `$http_host`。
- 使用 `BPMT_HTTP_PORT=18080` 等非 80 端口运行时，BPMT 生成的登录页、OAuth 授权页和第三方回调地址会保留实际端口。
- Maven 项目版本、默认 Web/API 镜像 tag、安装脚本默认 release tag 和源码 OpenAPI info 同步切到 `1.5.3`。

## 不变范围

- 不新增 OAuth 协议端点。
- 不修改 `bpmt-api` 业务接口。
- 不修改 OAuth 表结构。
- 不修改 `clientSecret` 的 hash-only 保存和重置规则。
- 不改动已经发布的 `v1.5.2` tag。

## 验收摘要

2026-05-04 已完成 `docs/v1.5.3/oauth-session-switch-acceptance.md` 记录的发布前验收：

- OAuth 登录态切换单测通过。
- Java 8 全仓编译通过，`docker compose config` 通过。
- 使用仓库内 `docker/nginx/nginx.conf` 在 `BPMT_HTTP_PORT=18080` 临时完整库运行态完成 OAuth 浏览器验收，未再使用临时 nginx 补丁。
- OAuth 浏览器验收覆盖未登录、已有登录态有权限、已有登录态无权限提示、取消返回 `access_denied`、切换账号后重新登录并继续授权。
- `v1.5.1` issue #10 回归继续通过：工作流待办“查看/处理”网络请求不出现 `_ORD_ID=null`。

## 发布产物

本次补丁的实际运行修复位于仓库 `docker/nginx/nginx.conf`，不在 Web/API 镜像层。直接上传本地新镜像到 GHCR 时，本机网络在最后的新 layer 上长时间无进展；因此发布镜像采用 registry 级别重标记方式生成，复用已发布并验证过的 `v1.5.2` Web/API 运行内容。

- Web image：`ghcr.io/wodenwang/bpmt-lite:1.5.3`
- Web digest：`sha256:6141d758529388af1231c1911455b4da662e59f64df71c268c9439af9372bdf7`
- API image：`ghcr.io/wodenwang/bpmt-lite-api:1.5.3`
- API digest：`sha256:70b415ddf2baad12c7abf771c3ddeb6bff82303e7cbebb7e02fdabdddcec1a24`
- `latest` 已同步到上述 digest。

说明：`v1.5.3` 的用户侧修复依赖 `v1.5.3` tag 中发布的 compose/nginx 配置；Web/API 容器运行内容与 `v1.5.2` 保持一致。
