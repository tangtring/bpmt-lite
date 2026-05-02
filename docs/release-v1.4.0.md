# v1.4.0 发布记录

`v1.4.0` 是 API 层版本。版本目标是在不改变现有 Web UI 技术栈和业务语义的前提下，新增独立 API 服务，优先让 AI agent、飞书集成平台、N8N 等外部系统可以清晰、稳定地管理 BPMT 动态表结构。

## 版本范围

- 新增独立 Maven 子模块 `api`，产物为 `api.war`。
- 新增独立 API Docker 镜像 `ghcr.io/wodenwang/bpmt-lite-api:1.4.0`。
- `web` 与 `api` 继续共用 MariaDB。
- `web` 与 `api` 各自内嵌 Hazelcast member，通过 compose 网络组网，不新增 Hazelcast Server 容器。
- 公开 API 文档：`http://127.0.0.1:8081/api/docs/`。
- 公开 OpenAPI：`http://127.0.0.1:8081/api/openapi.json`。
- 业务接口统一使用 HMAC-SHA256 签名认证。

## 首批 API

- `POST /api/v1/dynamic-tables`
- `GET /api/v1/dynamic-tables`
- `GET /api/v1/dynamic-tables/{name}`
- `PUT /api/v1/dynamic-tables/{name}`
- `POST /api/v1/dynamic-tables/{name}/sync-ddl`
- `GET /api/v1/dynamic-table-templates`

动态表 API 只管理结构，不管理业务数据；不暴露动态表删除接口。

## 认证与技术用户

- `BPMT_API_APP_KEY` 和 `BPMT_API_APP_SECRET` 通过 Docker compose 环境变量注入。
- 业务 API 请求使用 `X-BPMT-App-Key`、`X-BPMT-Timestamp`、`X-BPMT-Nonce`、`X-BPMT-Signature`。
- HMAC canonical path 包含公开 context path，例如 `/api/v1/dynamic-tables`。
- `BPMT_API_ACT_AS` 是固定技术用户；未配置或用户不可用时兜底 `admin`。

## 验收结论

- API 单测覆盖路由、JSON 解析、认证失败、错误响应结构、字段校验、技术用户兜底和错误码映射。
- `scripts/smoke-api.sh` 覆盖公开文档、未签名 401、签名列表、排序参数和不存在表 404。
- 2026-05-02 人工完整复测使用 `TMP_COWORK_V2`：总计 19 项，PASS 16 项，FAIL 0 项，SKIP 3 项。
- 人工复测确认 `PUT` 新增字段、`PUT` 不存在表 404、DDL 同步、模板列表和最终结构持久化均通过。
- 2026-05-02 本地发布 gate 已通过：仓库检查、compose 配置、Java 8 编译、API 单测、Web/API 镜像构建、API smoke 和 Hazelcast 双 member 验证。

详细结果见 [docs/v1.4.0/api-acceptance.md](v1.4.0/api-acceptance.md)。

## 发布信息

- Git tag：`v1.4.0`
- Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.4.0`，digest `sha256:d987bec603dbce23c5b3b1f5fdba787a79e7384a9efb93ae64453011298e2601`
- API 镜像：`ghcr.io/wodenwang/bpmt-lite-api:1.4.0`，digest `sha256:99bc848789baf9fc05cd6382994512e88c803769604d1cabd6d3434348037337`
- 同步镜像：`ghcr.io/wodenwang/bpmt-lite:latest`、`ghcr.io/wodenwang/bpmt-lite-api:latest`
- 匿名拉取验证：`1.4.0` 与 `latest` 均通过。
- 发布后独立临时 compose 验证：使用 `v1.4.0` raw `scripts/run.sh`、最小库 `bpmt_min`、发布后的 Web/API 镜像，在端口 `19080/19081` 验证 `/`、`/ueditor/`、`/api/openapi.json`、`/api/docs/` 和 `scripts/smoke-api.sh` 均通过；Web/API 日志确认 Hazelcast `Members [2]`。
