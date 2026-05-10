# v1.7.0 发布记录

## 版本定位

`v1.7.0` 是动态表视图配置 API 版本，目标是把原本依赖前端页面操作的 `dyn` 动态表视图配置能力开放给外部系统和 AI agent。

本版本只覆盖 `/{viewKey}.view` 对应的 dyn 视图配置，不发布菜单、首页卡片或按钮入口。

## 主要变化

- 新增 `GET /api/v1/dynamic-table-views` 和 `GET /api/v1/dynamic-table-views/{viewKey}`，支持列出和导出 dyn 视图快照。
- 新增 `POST /api/v1/dynamic-table-views:validate`，支持只校验并返回规范化快照。
- 新增 `dryRun=true`，覆盖创建、整体替换和分区 patch。
- 新增创建、整体替换、分区 patch 和带 `confirmViewKey` 的删除能力。
- 快照模型覆盖基础信息、字段、分组、页签、区块、系统按钮、自定义按钮、查询区、前后置处理器、预置变量、父页面变量、权限和脚本风险提示。
- 写接口只写视图配置和权限资源，不执行 DDL。
- 删除视图配置不会删除动态表、业务数据、日志表或日志数据。
- 公开文档和 OpenAPI 已归档到 `docs/v1.7.0/api-reference.md` 与 `docs/v1.7.0/openapi.json`。

## 非范围

- 不管理动态表业务数据 CRUD。
- 不删除动态表结构。
- 不发布或维护菜单、首页卡片、按钮入口。
- 不新增独立导航入口配置 API。
- 不支持在查询区、变量和处理器上写入权限；这些位置传入非空 `permissions` 会返回 `UNSUPPORTED_PERMISSION`。

## 升级说明

已有运行目录中执行：

```bash
cd bpmt-lite
sh ./upgrade.sh
```

`v1.6.2` 到 `v1.7.0` 不需要业务数据库结构变更。仓库包含空的升级标记 SQL：

```text
database/upgrade/v1.6.2-to-v1.7.0.sql
```

该文件只用于让升级脚本记录版本间升级步骤已评估，不创建或修改业务表。

## 验收摘要

验收记录见 [docs/v1.7.0/dynamic-table-view-acceptance.md](v1.7.0/dynamic-table-view-acceptance.md)。

本地已验证：

- `scripts/verify-repo.sh`
- `docker compose config`
- `mvn -s settings.local.xml -DskipTests compile`
- `mvn -s settings.local.xml -pl api -am '-Dtest=ApiDocsContractTest,ApiServletTest,DynamicTableView*Test' -DfailIfNoTests=false test`
- `mvn -s settings.local.xml -pl platform -am '-Dtest=*DynView*Test,*DynamicTableView*Test' -DfailIfNoTests=false test`
- `scripts/build-image.sh`
- `scripts/build-api-image.sh`
- 独立临时 compose smoke，覆盖 API 文档、OpenAPI、validate、dry-run、创建、导出、替换预检、分区 patch、视图入口、动态 Action、删除确认和删除后业务表保留。

本地镜像：

- `ghcr.io/wodenwang/bpmt-lite:1.7.0`
- `ghcr.io/wodenwang/bpmt-lite-api:1.7.0`

## 回滚说明

- 运行时可把 `BPMT_IMAGE_TAG` 和 `BPMT_API_IMAGE_TAG` 回退到 `1.6.2` 或 `latest` 中的上一稳定镜像。
- v1.7.0 没有业务数据库结构升级，回滚不需要执行反向 SQL。
- 如果已经通过 API 创建了新视图配置，回滚前应先用 `DELETE /api/v1/dynamic-table-views/{viewKey}?confirmViewKey={viewKey}` 删除不再需要的视图配置；删除不会影响底层动态表和业务数据。

## 发布边界

- 不升级 Java、Tomcat、MariaDB 或 nginx。
- 不改变 compose 中第三方容器版本策略。
- 不改变现有 OAuth、HTTPS、H5 和数据库操作 API 行为。
- 不把安装或升级状态写入业务数据库。
