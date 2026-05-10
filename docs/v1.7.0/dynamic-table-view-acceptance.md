# v1.7.0 动态表视图 API 验收记录

执行时间：2026-05-10。

## 验收范围

本次验收只覆盖 `dyn` 动态表视图配置 API：

- `GET /api/v1/dynamic-table-views`
- `POST /api/v1/dynamic-table-views:validate`
- `POST /api/v1/dynamic-table-views?dryRun=true`
- `POST /api/v1/dynamic-table-views`
- `GET /api/v1/dynamic-table-views/{viewKey}`
- `PUT /api/v1/dynamic-table-views/{viewKey}?dryRun=true`
- `PATCH /api/v1/dynamic-table-views/{viewKey}/{section}`
- `DELETE /api/v1/dynamic-table-views/{viewKey}?confirmViewKey=...`

不纳入菜单、首页卡片、按钮入口发布，也不纳入动态表业务数据 CRUD。

## 自动化检查

以下命令均在 Java 8 和 `settings.local.xml` 下执行：

```bash
scripts/verify-repo.sh
docker compose config
mvn -s settings.local.xml -DskipTests compile
mvn -s settings.local.xml -pl api -am '-Dtest=ApiDocsContractTest,ApiServletTest,DynamicTableView*Test' -DfailIfNoTests=false test
mvn -s settings.local.xml -pl platform -am '-Dtest=*DynView*Test,*DynamicTableView*Test' -DfailIfNoTests=false test
scripts/build-image.sh
scripts/build-api-image.sh
```

结果：

- 仓库检查：通过。
- Compose 配置解析：通过。
- 全仓编译：通过，Reactor 版本为 `1.7.0`。
- API 聚焦测试：通过，94 tests，0 failures，0 errors。
- Platform 聚焦测试：通过，没有匹配失败。
- Web 镜像构建：通过，生成并验证 `ghcr.io/wodenwang/bpmt-lite:1.7.0`。
- API 镜像构建：通过，生成并验证 `ghcr.io/wodenwang/bpmt-lite-api:1.7.0`。

## Compose Smoke

为避免影响本机已有 `bpmt-*` 容器，本次 smoke 使用 `/tmp` 下临时运行目录，并通过 override 改为独立容器名：

- `bpmt-v170-smoke-nginx`
- `bpmt-v170-smoke-web`
- `bpmt-v170-smoke-api`
- `bpmt-v170-smoke-mariadb`

端口：

- HTTP：`18080`
- MariaDB：`13306`

初始化库：

- `database/bpmt-min.sql.gz`
- `DB_NAME=bpmt_min`

基础入口结果：

| 地址 | 状态 |
| --- | --- |
| `http://127.0.0.1:18080/` | `200` |
| `http://127.0.0.1:18080/ueditor/` | `200` |
| `http://127.0.0.1:18080/api/docs/` | `200` |
| `http://127.0.0.1:18080/api/openapi.json` | `200`，包含 `dynamic-table-views` |

Hazelcast 集群：

- Web/API 均启动成功。
- 日志出现 `Members [2]`，包含 `bpmt-web` 与 `bpmt-api`。

## 视图生命周期

本次 smoke 生成的动态表和视图：

```text
TABLE=TMP_CDX_V170_0510231523
VIEW=TMP_CDX_V170_0510231523_VIEW
```

数据库表数量：

| 时点 | 表数量 |
| --- | ---: |
| 最小库初始化后 | 176 |
| 创建测试动态表、删除视图配置后 | 177 |

业务数据保留：

| 项 | 数量 |
| --- | ---: |
| 删除视图前测试表业务行 | 1 |
| 删除视图后测试表业务行 | 1 |

API 生命周期结果：

| 操作 | 结果 |
| --- | --- |
| `POST /api/v1/dynamic-tables` 创建测试动态表 | `200`，`success=true` |
| `POST /api/v1/dynamic-table-views:validate` | `200`，`success=true`，`data.valid=true` |
| `POST /api/v1/dynamic-table-views?dryRun=true` | `200`，`success=true`，`data.plan.dryRun=true` |
| dry-run 后查询 `VW_URL` | `0`，未落库 |
| `POST /api/v1/dynamic-table-views` | `200`，`success=true` |
| `GET /api/v1/dynamic-table-views/{viewKey}` | `200`，`success=true` |
| `PUT /api/v1/dynamic-table-views/{viewKey}?dryRun=true` | `200`，`success=true`，`data.plan.dryRun=true` |
| `PATCH /api/v1/dynamic-table-views/{viewKey}/fields` | `200`，`success=true` |
| `GET /{viewKey}.view` | `200` |
| `GET /dyn/A{viewKey}Action/list.shtml` | `200` |
| `DELETE /api/v1/dynamic-table-views/{viewKey}` | `400`，`DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED` |
| `DELETE /api/v1/dynamic-table-views/{viewKey}?confirmViewKey={viewKey}` | `200`，`success=true` |

脱敏响应摘要：

```json
{"success":true,"valid":true}
{"success":true,"dryRun":true,"viewKey":"TMP_CDX_V170_0510231523_VIEW"}
{"success":false,"code":"DYNAMIC_TABLE_VIEW_CONFIRM_REQUIRED"}
{"success":true}
```

## 结论

- 动态表视图 API 可以完成 validate、dry-run、创建、导出、替换预检、分区 patch 和带确认删除。
- `dryRun=true` 不写入 `VW_URL`。
- 删除视图配置不会删除底层动态表和业务数据。
- 菜单、首页、按钮入口发布不属于 v1.7.0 范围。
- smoke 临时容器已在验收后关闭。
