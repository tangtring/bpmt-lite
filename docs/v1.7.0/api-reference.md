# v1.7.0 API 文档

本文是 `bpmt-lite` v1.7.0 的 API 归档版文档，面向人类阅读。机器可读的 OpenAPI 快照见 [openapi.json](openapi.json)。

运行中的公开文档入口：

- Web 文档：`http://127.0.0.1/api/docs/`
- OpenAPI：`http://127.0.0.1/api/openapi.json`

## 版本定位

v1.7.0 在既有动态表结构 API 和数据库操作 API 基础上，新增动态表视图 API。

动态表视图 API 的边界：

- 只管理 dyn 动态表视图。
- 只维护 `/{viewKey}.view` 对应视图配置。
- 不纳入菜单、首页、按钮入口等导航配置。
- 删除视图不会删除动态表和业务数据，也不会删除日志表或日志数据。
- 所有写接口只写 BPMT 视图配置和权限资源，不执行 DDL。

## 认证方式

业务 API 使用 `appKey/appSecret` 的 HMAC-SHA256 签名。默认本地开发配置为：

```text
BPMT_API_APP_KEY=bpmt-api
BPMT_API_APP_SECRET=bpmt-api-secret
BPMT_API_ACT_AS=admin
```

正式部署必须覆盖默认 `appSecret`。请求头：

```text
X-BPMT-App-Key
X-BPMT-Timestamp
X-BPMT-Nonce
X-BPMT-Signature
```

签名原文：

```text
METHOD
PATH
NORMALIZED_QUERY
TIMESTAMP
NONCE
SHA256_HEX(BODY)
```

规则：

- `PATH` 必须包含公开 context path，例如 `/api/v1/dynamic-table-views`。
- `NORMALIZED_QUERY` 按解码后的参数名和值排序，再 URL encode；无 query 时为空行。
- `BODY` 为空时使用空字符串计算 SHA-256。
- `appSecret` 不允许出现在 query 或 request body 中。

## 快照模型

动态表视图快照的顶层结构：

```json
{
  "viewKey": "CRM_CUSTOMER_VIEW",
  "description": "客户视图",
  "loginRequired": true,
  "base": {
    "tableName": "CRM_CUSTOMER",
    "displayName": "客户",
    "layoutColumns": 2,
    "initQuery": true,
    "pageLimit": 20
  },
  "fields": {
    "systemFields": [],
    "computedFields": [],
    "formFields": [],
    "sectionLines": [],
    "listOrder": []
  },
  "queries": {
    "normal": [],
    "advanced": []
  },
  "limits": [],
  "variables": {
    "prepared": [],
    "parents": []
  },
  "processors": {
    "before": [],
    "after": []
  },
  "subviews": {
    "systemTabs": [],
    "viewTabs": []
  },
  "buttons": {
    "system": [],
    "item": [],
    "summary": []
  },
  "weixin": null,
  "scripts": {}
}
```

`GET /api/v1/dynamic-table-views/{viewKey}` 导出的 `snapshot` 可以作为 `PUT` 替换的 baseline。外部系统建议先导出、修改目标 section、再使用 `validate` 或 `dryRun=true` 检查计划。

## 接口清单

业务前缀为 `/api/v1`。

| 方法 | 路径 | 说明 | 风险 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/dynamic-table-views` | 分页列出 dyn 动态表视图 | 只读 |
| `POST` | `/api/v1/dynamic-table-views?dryRun=true` | 创建 dyn 动态表视图，`dryRun` 是 query 参数 | 写元数据，不执行 DDL |
| `POST` | `/api/v1/dynamic-table-views:validate` | 校验并规范化快照，不落库 | 只读 |
| `GET` | `/api/v1/dynamic-table-views/{viewKey}` | 导出完整快照 | 只读 |
| `PUT` | `/api/v1/dynamic-table-views/{viewKey}?dryRun=true` | 完整替换快照，`dryRun` 是 query 参数 | 写元数据，不执行 DDL |
| `PATCH` | `/api/v1/dynamic-table-views/{viewKey}/{section}?dryRun=true` | 局部替换 section，没有额外路径前缀 | 写元数据，不执行 DDL |
| `DELETE` | `/api/v1/dynamic-table-views/{viewKey}?confirmViewKey=...` | 删除视图配置，需要确认参数 | 写元数据，不执行 DDL |

可 patch 的 `section`：

```text
base
fields
queries
limits
processors
variables
subviews
buttons
weixin
scripts
```

写接口在 OpenAPI 中均标注：

```json
{
  "x-bpmt-writes-metadata": true,
  "x-bpmt-executes-ddl": false,
  "x-bpmt-risk-level": "high"
}
```

## validate 与 dryRun

`validate` 只校验并返回规范化快照：

```text
POST /api/v1/dynamic-table-views:validate
```

`dryRun=true` 支持创建、替换和 section patch：

```text
POST /api/v1/dynamic-table-views?dryRun=true
PUT /api/v1/dynamic-table-views/{viewKey}?dryRun=true
PATCH /api/v1/dynamic-table-views/{viewKey}/{section}?dryRun=true
```

`dryRun=true` 响应中的 `plan.dryRun` 为 `true`，并列出将创建、更新、删除的配置项和权限资源，但不会写入数据库。

## 权限资源

权限限制：

- 只有底层 dyn HBM 有 `PRI` 的字段、分割线、限制、子视图、按钮、微信等支持 `permissions`。
- 查询、变量、处理器 `permissions` 会被校验拒绝。
- 被拒绝时返回 `UNSUPPORTED_PERMISSION`，错误路径会指向具体字段，例如 `queries.normal[0].permissions`。

权限资源由 API 保留或生成，典型格式：

```text
dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.view
dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.create
dyn.CRM_CUSTOMER_VIEW.field.CUSTOMER_ID.update
```

## 脚本风险

动态表视图中的脚本字段会进行风险扫描，包括列表脚本、表单脚本、字段脚本、查询脚本、变量脚本和处理器脚本。

包含删除、反射、进程执行、文件操作、直接数据库删除等高风险片段时，API 会通过 `warnings` 返回风险提示。外部系统应先调用 `validate` 或使用 `dryRun=true` 获取 warnings，再人工复核。

## 错误码

常见错误码：

| code | 含义 |
| --- | --- |
| `DYNAMIC_TABLE_VIEW_NOT_FOUND` | 视图不存在 |
| `DYNAMIC_TABLE_VIEW_NOT_DYN` | 目标视图不是 dyn 动态表视图 |
| `DYNAMIC_TABLE_VIEW_INVALID_SNAPSHOT` | 快照结构、字段引用或 section 内容不合法 |
| `DYNAMIC_TABLE_VIEW_ALREADY_EXISTS` | 创建时 viewKey 已存在 |
| `DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED` | 删除时缺少 `confirmViewKey` 或确认值不一致 |
| `UNSUPPORTED_PERMISSION` | 不支持 permissions 的区块传入了 permissions |
| `API_INVALID_PARAMETER` | 分页或 query 参数不合法 |
| `API_AUTH_FAILED` | HMAC 认证失败 |

错误响应仍使用统一包装：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED",
    "message": "删除动态表视图必须传入 confirmViewKey 并与 viewKey 一致。",
    "details": {},
    "requestId": "..."
  }
}
```

## curl 示例

导出快照：

```bash
BASE_URL="http://127.0.0.1/api"
APP_KEY="bpmt-api"
APP_SECRET="bpmt-api-secret"
METHOD="GET"
PATH="/api/v1/dynamic-table-views/CRM_CUSTOMER_VIEW"
QUERY=""
BODY=""
TIMESTAMP="$(date +%s)"
NONCE="demo-$TIMESTAMP"
BODY_HASH="$(printf '%s' "$BODY" | shasum -a 256 | awk '{print $1}')"
CANONICAL="$(printf '%s\n%s\n%s\n%s\n%s\n%s' "$METHOD" "$PATH" "$QUERY" "$TIMESTAMP" "$NONCE" "$BODY_HASH")"
SIGNATURE="$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" | awk '{print $NF}')"

curl -sS "$BASE_URL/v1/dynamic-table-views/CRM_CUSTOMER_VIEW" \
  -H "X-BPMT-App-Key: $APP_KEY" \
  -H "X-BPMT-Timestamp: $TIMESTAMP" \
  -H "X-BPMT-Nonce: $NONCE" \
  -H "X-BPMT-Signature: $SIGNATURE"
```

删除视图配置：

```bash
curl -sS "$BASE_URL/v1/dynamic-table-views/CRM_CUSTOMER_VIEW?confirmViewKey=CRM_CUSTOMER_VIEW" \
  -X DELETE \
  -H "X-BPMT-App-Key: $APP_KEY" \
  -H "X-BPMT-Timestamp: $TIMESTAMP" \
  -H "X-BPMT-Nonce: $NONCE" \
  -H "X-BPMT-Signature: $SIGNATURE"
```

## 给外部 AI agent 的使用建议

- 先读取 [openapi.json](openapi.json)，按 OpenAPI 中的真实路由生成工具，不要推断额外路径。
- 修改前先 `GET /api/v1/dynamic-table-views/{viewKey}` 导出完整快照。
- 小范围修改优先使用 `PATCH /api/v1/dynamic-table-views/{viewKey}/{section}?dryRun=true`。
- 正式写入前至少执行一次 `validate` 或 `dryRun=true`，检查 `warnings`、`errors` 和 `plan`。
- 不要向查询、变量、处理器写入 `permissions`。
- 删除视图前确认只删除视图配置，不删除动态表和业务数据。
