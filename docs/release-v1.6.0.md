# v1.6.0 发布记录

## 发布定位

`v1.6.0` 新增 `bpmt-lite` HTTPS 支持，让 Web、UEditor、OAuth、H5 和 API 都能在 HTTPS 公开入口下正确运行，同时保留默认 HTTP 快速启动体验。

本版本支持两种 HTTPS 部署方式：

- 内置 nginx TLS：通过 `BPMT_HTTPS_ENABLED=1` 和 `docker-compose.https.yml` 启用。
- 可信上游 TLS：由上游网关终止 TLS 后转发到本 nginx HTTP 入口，并通过 `BPMT_UPSTREAM_TLS_ENABLED=1` 渲染公开 `https` 代理头。

## 变更内容

- Maven 项目版本切到 `1.6.0`。
- 默认 Web/API 镜像 tag 切到 `1.6.0`。
- `scripts/install.sh`、`scripts/run.sh`、`scripts/init-db.sh` 默认 release/raw tag 切到 `v1.6.0`。
- 新增 `docker-compose.https.yml`、nginx 配置渲染脚本和自签证书生成脚本。
- 后端公开 URL 生成统一读取可信 `X-Forwarded-Proto`、`X-Forwarded-Host`、`X-Forwarded-Port`。
- OAuth `/oauth/authorize` 使用公网 request URI/query 生成内部 `_full_url`，不信任外部传入的 `_full_url` 参数。
- H5 和微信绑定页面移除 HTTPS mixed content 阻断点。
- `scripts/smoke-api.sh` 支持自签证书 HTTPS API smoke。
- README、维护文档、AGENTS 和 v1.6.0 验收记录同步更新。

## 验收摘要

2026-05-05 已完成发布验收：

- `scripts/verify-repo.sh` 通过。
- `docker compose config` 通过，默认 compose 不发布 443。
- Java 8 全仓编译通过：`mvn -s settings.local.xml -DskipTests compile`。
- API 单测通过：39 项，FAIL 0，ERROR 0。
- OAuth/HTTPS 目标单测通过：`ActionsForwardedUrlTest`、`OAuthActionTest`、`HttpsStaticResourceTest`。
- 本地 Web/API 镜像构建通过：`scripts/build-image.sh`、`scripts/build-api-image.sh`。
- HTTPS 最小库运行验收通过：`/`、`/ueditor/`、`/api/docs/`、`/api/openapi.json` 均返回 200，HTTP 到 HTTPS 301 正确，`bpmt_min` 为 176 张表。
- HTTPS 完整库运行验收通过：`bpmt` 为 380 张表，API HTTPS smoke 通过。
- v1.5.1 issue #10 回归通过：工作流待办“查看/处理”未出现 `_ORD_ID=null`。
- v1.5.2 OAuth 登录态切换回归通过：无权限提示、取消返回、切换账号后继续授权均通过。
- H5 代表业务路径在 HTTPS 下可浏览，未出现 HTTP CDN mixed content 阻断。
- multi-arch 发布通过：`scripts/build-multiarch-images.sh`。

## 发布产物

- Web image：`ghcr.io/wodenwang/bpmt-lite:1.6.0`
- Web manifest digest：`sha256:65409ca2ab7d187cb71bc1a8ba89a08058a83ccadd7ab72787bfdc8e7b463605`
- Web amd64 digest：`sha256:f06ae5f5a872ba9db706626db547fe999e08e19b66ae9eaf51b6e816302e2dcc`
- Web arm64 digest：`sha256:8e600164d9dcb614e8e1b4c6e20ec999f63ce0d0e8b7ed1c9c3bfc7893761252`
- API image：`ghcr.io/wodenwang/bpmt-lite-api:1.6.0`
- API manifest digest：`sha256:c7ad44f0fd6e0b9d96aa8d555512a3d85017686675c4c921720fb024a8a39452`
- API amd64 digest：`sha256:fabf7b586fe9b6d608b4008d3eae04c9187d4c5f5605759bc420955becc19e4e`
- API arm64 digest：`sha256:319418992a2f66f87fa63b461e94b3be36a9f6a316d89937a8ea64c7f8b17283`
- `latest` 已同步到上述 Web/API manifest digest。

## 后续公开验证

发布 tag 后需用公开 tag 路径执行一键安装验证：

```bash
curl -fsSL https://github.com/wodenwang/bpmt-lite/raw/refs/tags/v1.6.0/scripts/install.sh | BPMT_HTTPS_ENABLED=1 bash
```
