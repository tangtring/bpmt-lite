# v1.4.0 API 验收清单

## 基线

- Web：`http://127.0.0.1:8080/`
- API：`http://127.0.0.1:8081/api/`
- OpenAPI：`http://127.0.0.1:8081/api/openapi.json`
- Web 文档：`http://127.0.0.1:8081/api/docs/`
- API 认证：HMAC-SHA256
- 技术用户：`BPMT_API_ACT_AS`，缺省或不存在时兜底 `admin`
- Web/API 缓存：两个容器各自内嵌 Hazelcast，通过 compose 网络组成同一集群

当前实现分支的 Maven 版本仍是 `1.3.0`，本地构建镜像 tag 也会跟随为 `1.3.0`。正式发布 v1.4.0 时再统一切换 Maven 版本、`BPMT_IMAGE_TAG`、`BPMT_API_IMAGE_TAG` 和 GHCR tag。

## 必过项

- `scripts/verify-repo.sh` 通过。
- `mvn -s settings.local.xml -DskipTests compile` 通过。
- `mvn -s settings.local.xml -pl api -am -Dtest=ApiDocsContractTest,ApiServletTest,DynamicTableControllerTest,DynamicTableServiceTest,DynamicTableValidatorTest,HmacSignatureTest,ApiUserContextTest -DfailIfNoTests=false test` 通过。
- `docker compose config` 通过。
- `scripts/build-image.sh` 通过。
- `scripts/build-api-image.sh` 通过。
- `/api/openapi.json` 返回 200。
- `/api/docs/` 返回 200。
- 无签名访问 `/api/v1/dynamic-tables` 返回 401。
- 正确签名访问 `/api/v1/dynamic-tables` 返回 200，且响应 `success=true`。
- 正确签名访问 `/api/v1/dynamic-tables?order=desc&sort=createDate` 返回 200，且响应中包含 `sort=createDate`、`order=desc`。
- 正确签名 `PUT /api/v1/dynamic-tables/{不存在表}` 返回 404 和 `DYNAMIC_TABLE_NOT_FOUND`，不能返回 500。
- API 创建测试动态表后，MariaDB 中存在物理表和 `tb_table`、`tb_column` 元数据。
- API 调整测试动态表时必须走 `TableService` 安全 DDL 路径；新增普通 `String` 字段不能触发 `INTERNAL_ERROR`。
- API 调整测试动态表后，不重启 Web，Web 动态表管理页可读到新结构。
- Web 和 API 日志显示 Hazelcast 加入同一集群。

## 2026-05-02 人工测试问题回归项

- BUG-1：`PUT` 修改表结构时，API 已禁止走 `TableService` 的粗暴 DDL 路径，统一使用安全 DDL；新增普通 `String` 字段应成功。
- BUG-2：`PUT` 不存在的动态表必须在 DDL 前检查 `tb_table` 元数据，并返回 404。
- BUG-3：列表接口新增 `sort`、`order` 参数，允许字段为 `name`、`description`、`createDate`、`updateDate`、`cacheFlag`；默认 `createDate desc`，方便优先看到新建表。

当前回归结果：

- API 镜像重建后，`scripts/smoke-api.sh` 通过，已覆盖排序列表和不存在表 404。
- 临时动态表 `RV_API_FIX_200454` 创建成功。
- `PUT /api/v1/dynamic-tables/RV_API_FIX_200454` 新增普通 `String` 字段 `ADDED_STR` 成功返回 200。
- MariaDB 验证结果：物理表存在、物理列 `ADDED_STR` 存在、`tb_table` 元数据存在、`tb_column` 中 `ADDED_STR` 元数据存在。
- API 容器 `platform.log` 显示 `TableService` 执行的是“使用安全模式改表”，没有再进入“粗暴模式改表”。

## 明确不验收

- 动态表业务数据 CRUD。
- 删除动态表。
- OAuth2/OIDC。
- 独立 Hazelcast Server 容器。
- 第三方平台适配器封装。
