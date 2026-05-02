# v1.4.0 API 开发规范

本文是 `bpmt-lite` 后续 API 开发的统一规范。新增或调整 API 时，必须同步更新 OpenAPI、Web 文档和单测。

## 模块边界

- API 子项目固定命名为 `api`。
- API 可依赖 `platform`、`util`、`dbtools` 等现有模块。
- 原则上不修改 `platform` 及以上项目源码；确需修改时必须说明原因并补回归验证。
- `v1.4.0` 只开放结构管理能力，不开放动态表业务数据 CRUD。
- 动态表删除、批量 DDL、直接 SQL 执行等危险能力默认不暴露。

## URL 和文档

- 业务接口统一挂载在 `/api/v1/*`。
- 公开文档不需要认证：
  - `/api/openapi.json`
  - `/api/docs/`
- 每个对外接口必须在 `api/src/main/webapp/openapi.json` 中描述清楚。
- 文档面向人和 AI，描述必须包含请求参数、响应结构、错误响应和安全要求。
- 每个 OpenAPI operation 必须标注：
  - `x-bpmt-writes-metadata`
  - `x-bpmt-executes-ddl`
  - `x-bpmt-risk-level`

## 认证和技术用户

- 业务接口统一使用 appKey/appSecret 的 HMAC-SHA256 签名。
- 调用方必须传：
  - `X-BPMT-App-Key`
  - `X-BPMT-Timestamp`
  - `X-BPMT-Nonce`
  - `X-BPMT-Signature`
- 签名 canonical string 固定为：

```text
METHOD
PATH
NORMALIZED_QUERY
TIMESTAMP
NONCE
SHA256_HEX(BODY)
```

- `PATH` 使用公开请求 URI，例如 `/api/v1/dynamic-tables`，不能只签 `/v1/dynamic-tables`。
- `NORMALIZED_QUERY` 按解码后的参数名和值排序，再使用 URL encoding，空 query 为空字符串。
- `X-BPMT-Timestamp` 使用 Unix 秒级时间戳，默认允许 300 秒时钟偏差。
- `BPMT_API_APP_KEY` 和 `BPMT_API_APP_SECRET` 从 Docker compose 环境变量注入，正式部署必须覆盖默认值。
- `BPMT_API_ACT_AS` 是固定技术用户；未配置或用户不可用时兜底 `admin`。

## 响应结构

成功响应统一为：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

失败响应统一为：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "STABLE_ERROR_CODE",
    "message": "可读错误描述",
    "details": {},
    "requestId": "uuid"
  }
}
```

- 错误码必须稳定，方便 AI agent 和第三方平台判断。
- 对外接口不直接暴露 Java 异常栈。
- 对外 DTO 不直接返回 ORM 或 JumpMind 内部对象；需要转换成 API DTO。

## 动态表接口

`v1.4.0` 首批接口：

- `GET /api/v1/dynamic-tables`
- `POST /api/v1/dynamic-tables`
- `GET /api/v1/dynamic-tables/{name}`
- `PUT /api/v1/dynamic-tables/{name}`
- `POST /api/v1/dynamic-tables/{name}/sync-ddl`
- `GET /api/v1/dynamic-table-templates`

动态表结构写入必须同时考虑：

- 物理 DDL
- `TB_TABLE`
- `TB_COLUMN`
- `TB_INDEX`
- Web 与 API 两个 Tomcat 实例中的 Hibernate/Hazelcast 缓存一致性

## Docker 和缓存

- API 使用独立 Docker 容器，不进入 Web 容器。
- Web 和 API 各自内嵌 Hazelcast，不引入独立 Hazelcast Server 容器。
- compose 内通过 `HAZELCAST_TCPIP=true` 和 `HAZELCAST_TCPIP_MEMBERS=web,api` 组网。
- 缓存不能关闭，`HIBERNATE_CACHE` 保持 `true`。
- Web 和 API 使用同一数据库配置、同一 Hazelcast group name/password。

## 测试要求

- 每个对外接口必须有单测。
- 认证、签名、错误响应、DTO 转换、动态表结构校验必须有单测。
- OpenAPI 和 Web 文档发布必须有契约测试。
- 提交前至少运行：

```bash
mvn -s settings.local.xml -pl api -am -Dtest=ApiDocsContractTest,ApiServletTest,DynamicTableControllerTest,DynamicTableServiceTest,DynamicTableValidatorTest,HmacSignatureTest,ApiUserContextTest -DfailIfNoTests=false test
docker compose config
```

涉及 Docker 的改动还必须运行：

```bash
scripts/build-api-image.sh
```

本地运行联调时执行：

```bash
scripts/smoke-api.sh
```
