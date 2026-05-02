# v1.4.0 API 验收清单

## 基线

- Web：`http://127.0.0.1:8080/`
- API：`http://127.0.0.1:8081/api/`
- OpenAPI：`http://127.0.0.1:8081/api/openapi.json`
- Web 文档：`http://127.0.0.1:8081/api/docs/`
- API 认证：HMAC-SHA256
- 技术用户：`BPMT_API_ACT_AS`，缺省或不存在时兜底 `admin`
- Web/API 缓存：两个容器各自内嵌 Hazelcast，通过 compose 网络组成同一集群

当前发布收口分支的 Maven 版本、`BPMT_IMAGE_TAG`、`BPMT_API_IMAGE_TAG` 均已切换为 `1.4.0`。

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

2026-05-02 20:17:23 人工完整复测：

- API 地址：`http://127.0.0.1:8081/api`
- 测试表：`TMP_COWORK_V2`
- 认证方式：HMAC-SHA256，`appKey=bpmt-api`
- 结果汇总：总计 19 项，PASS 16 项，FAIL 0 项，SKIP 3 项。
- 覆盖接口：`POST /v1/dynamic-tables`、`GET /v1/dynamic-tables`、`GET /v1/dynamic-tables/{name}`、`PUT /v1/dynamic-tables/{name}`、`POST /v1/dynamic-tables/{name}/sync-ddl`、`GET /v1/dynamic-table-templates`。
- 通过项包括：重复创建 409、无主键拒绝、禁用前缀拒绝、分页查询、单表查询、不存在表 404、PUT 新增字段持久化、PUT 不存在表 404、DDL 同步、同步不存在表 404、仅修改描述、模板列表、最终结构校验。
- 跳过项均为幂等场景：`TMP_COWORK_V2` 已存在、`DIAG_STR` 已存在、`DIAG2_STR` 已存在。
- 最终结构校验确认 `EMAIL_STR`、`ACTIVE_FLAG_INT` 等字段存在。

2026-05-02 20:28 本地发布 gate：

- `scripts/verify-repo.sh` 通过。
- `docker compose config` 通过，Web/API 默认镜像解析为 `1.4.0`。
- `git diff --check` 通过。
- Java 8 下 `mvn -s settings.local.xml -DskipTests compile` 通过，Reactor 版本为 `1.4.0`。
- Java 8 下 API 单测通过：36 项，FAIL 0。
- `scripts/build-image.sh` 通过，生成并验证 `ghcr.io/wodenwang/bpmt-lite:1.4.0`。
- `scripts/build-api-image.sh` 通过，生成并验证 `ghcr.io/wodenwang/bpmt-lite-api:1.4.0`。
- 使用 `ghcr.io/wodenwang/bpmt-lite-api:1.4.0` 替换本地 8081 API smoke 容器后，`scripts/smoke-api.sh` 通过。
- API 与 Web 日志均确认 Hazelcast `Members [2]`，成员为 `web-api-smoke` 与 `api-cluster-smoke`。
- GHCR 推送和匿名拉取验证通过：Web digest `sha256:d987bec603dbce23c5b3b1f5fdba787a79e7384a9efb93ae64453011298e2601`，API digest `sha256:99bc848789baf9fc05cd6382994512e88c803769604d1cabd6d3434348037337`。
- 发布后独立临时 compose 验证通过：使用 `v1.4.0` raw `scripts/run.sh` 下载 compose 和最小库 SQL，手动避开本机已有固定容器名后，以 `DB_NAME=bpmt_min`、端口 `19080/19081/19306` 拉起发布镜像；`/`、`/ueditor/`、`/api/openapi.json`、`/api/docs/`、`scripts/smoke-api.sh` 和 Hazelcast 双 member 均通过。

## 明确不验收

- 动态表业务数据 CRUD。
- 删除动态表。
- OAuth2/OIDC。
- 独立 Hazelcast Server 容器。
- 第三方平台适配器封装。
