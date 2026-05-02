# v1.4.0 API 文档

本文是 `bpmt-lite` v1.4.0 的 API 归档版文档，面向人类阅读。机器可读的 OpenAPI 快照见 [openapi.json](openapi.json)。

运行中的公开文档入口：

- Web 文档：`http://127.0.0.1:8081/api/docs/`
- OpenAPI：`http://127.0.0.1:8081/api/openapi.json`

## 范围

v1.4.0 只开放动态表结构管理能力：

- 创建动态表结构。
- 查询动态表列表和详情。
- 调整动态表字段结构。
- 同步动态表 DDL。
- 查询动态表模板列表。

本版本不开放动态表删除接口，不开放动态表业务数据 CRUD，不开放业务数据导入导出。

动态表结构写接口会同时修改数据库 DDL 和 BPMT 元数据表，包括 `tb_table`、`tb_column`、`tb_index`、`tb_index_column` 等。

## 认证

业务 API 使用 `appKey/appSecret` 的 HMAC-SHA256 签名。默认本地开发配置为：

```text
BPMT_API_APP_KEY=bpmt-api
BPMT_API_APP_SECRET=bpmt-api-secret
BPMT_API_ACT_AS=admin
```

正式部署必须覆盖默认 `appSecret`。`BPMT_API_ACT_AS` 是固定技术用户，未配置或用户不可用时兜底 `admin`。

请求头：

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

- `METHOD` 使用大写 HTTP 方法，例如 `GET`、`POST`、`PUT`。
- `PATH` 必须包含公开 context path，例如 `/api/v1/dynamic-tables`。
- `NORMALIZED_QUERY` 按解码后的参数名和值排序，再 URL encode；无 query 时为空行。
- `BODY` 为空时使用空字符串计算 SHA-256。
- `appSecret` 不允许出现在 query 或 request body 中。

## 响应格式

成功响应：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

错误响应：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "DYNAMIC_TABLE_NOT_FOUND",
    "message": "动态表不存在。",
    "details": {},
    "requestId": "..."
  }
}
```

常见 HTTP 状态：

| 状态 | 含义 |
| --- | --- |
| `400` | JSON、字段类型或必填参数错误 |
| `401` | 缺少认证信息、appKey 不存在或签名错误 |
| `403` | 技术用户不可用或权限不足 |
| `404` | 动态表或模板不存在 |
| `409` | 表、字段或索引冲突 |
| `422` | 动态表规则校验或 DDL 执行失败 |
| `500` | 未预期系统异常 |

## 接口

业务前缀为 `/api/v1`。

| 方法 | 路径 | 说明 | 风险 |
| --- | --- | --- | --- |
| `GET` | `/api/v1/dynamic-tables` | 查询动态表结构列表 | 只读 |
| `POST` | `/api/v1/dynamic-tables` | 创建动态表结构 | 写元数据，执行 DDL |
| `GET` | `/api/v1/dynamic-tables/{name}` | 查询单个动态表结构 | 只读 |
| `PUT` | `/api/v1/dynamic-tables/{name}` | 调整动态表结构 | 写元数据，执行 DDL |
| `POST` | `/api/v1/dynamic-tables/{name}/sync-ddl` | 同步动态表 DDL | 执行 DDL |
| `GET` | `/api/v1/dynamic-table-templates` | 查询动态表模板列表 | 只读 |

### 查询动态表列表

```text
GET /api/v1/dynamic-tables?start=0&limit=20&sort=createDate&order=desc
```

参数：

| 参数 | 默认 | 说明 |
| --- | --- | --- |
| `start` | `0` | 起始偏移 |
| `limit` | `20` | 返回数量，最大 `100` |
| `sort` | `createDate` | 可选 `name`、`description`、`createDate`、`updateDate`、`cacheFlag` |
| `order` | `desc` | 可选 `asc`、`desc` |

### 创建或调整动态表

请求结构：

```json
{
  "name": "TMP_EXAMPLE",
  "description": "示例动态表",
  "cacheFlag": 0,
  "columns": [
    {
      "name": "ID",
      "description": "主键",
      "type": "String",
      "totalSize": 64,
      "primaryKey": true,
      "required": true
    },
    {
      "name": "NAME",
      "description": "名称",
      "type": "String",
      "totalSize": 100
    }
  ],
  "indexes": [
    {
      "name": "IDX_TMP_EXAMPLE_NAME",
      "columns": ["NAME"]
    }
  ]
}
```

字段类型：

| 类型 | 说明 |
| --- | --- |
| `String` | 字符串，通常需要 `totalSize` |
| `Integer` | 整数 |
| `BigDecimal` | 数值，支持 `totalSize` 和 `scale` |
| `Date` | 日期时间 |
| `Long` | 长整数 |
| `Clob` | 大文本 |
| `Blob` | 二进制 |

规则：

- `name` 必须以字母开头，只允许字母、数字和下划线。
- 至少需要一个 `primaryKey=true` 字段。
- 系统表前缀默认禁止通过 API 创建或调整。
- `PUT /api/v1/dynamic-tables/{name}` 以 path 中的 `{name}` 为准。
- 不存在的动态表执行 `PUT` 时返回 `404` 和 `DYNAMIC_TABLE_NOT_FOUND`。

### 同步 DDL

```text
POST /api/v1/dynamic-tables/{name}/sync-ddl
```

该接口会先确认动态表元数据存在，再执行现有 BPMT DDL 同步逻辑。不存在的动态表返回 `404`。

## curl 签名示例

下面示例查询最新动态表列表：

```bash
BASE_URL="http://127.0.0.1:8081/api"
APP_KEY="bpmt-api"
APP_SECRET="bpmt-api-secret"
METHOD="GET"
PATH="/api/v1/dynamic-tables"
QUERY="order=desc&sort=createDate"
BODY=""
TIMESTAMP="$(date +%s)"
NONCE="demo-$TIMESTAMP"
BODY_HASH="$(printf '%s' "$BODY" | shasum -a 256 | awk '{print $1}')"
CANONICAL="$(printf '%s\n%s\n%s\n%s\n%s\n%s' "$METHOD" "$PATH" "$QUERY" "$TIMESTAMP" "$NONCE" "$BODY_HASH")"
SIGNATURE="$(printf '%s' "$CANONICAL" | openssl dgst -sha256 -hmac "$APP_SECRET" | awk '{print $NF}')"

curl -sS "$BASE_URL/v1/dynamic-tables?$QUERY" \
  -H "X-BPMT-App-Key: $APP_KEY" \
  -H "X-BPMT-Timestamp: $TIMESTAMP" \
  -H "X-BPMT-Nonce: $NONCE" \
  -H "X-BPMT-Signature: $SIGNATURE"
```

## 对 AI agent 的约束

- 优先读取 [openapi.json](openapi.json)，不要从 HTML 页面反推接口。
- 所有业务 API 都要签名，文档端点除外。
- 写接口会执行 DDL，调用前必须明确目标表、字段和索引结构。
- 不要尝试调用删除动态表或业务数据 CRUD；v1.4.0 未暴露这些能力。
- 动态表结构调整失败时，应优先读取错误响应中的 `error.code` 和 `error.requestId`。
