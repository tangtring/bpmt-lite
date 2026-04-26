# bpmt-lite v1.1.0 发布说明

`v1.1.0` 目标是继续收口 bpmt-lite 的公开发行体验，让维护者更容易构建，让使用者更容易完成一次可用的本地初始化。

## 版本信息

- Git tag：`v1.1.0`
- 默认 Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.1.0`
- 同步镜像 tag：发布后同步到 `ghcr.io/wodenwang/bpmt-lite:latest`
- 默认访问地址：`http://127.0.0.1:8080/`
- 数据库名：`kyq`

## 主要变化

- 移除或替换阻断公开 Maven 构建的历史依赖。
- 默认发行裁剪 Aspose、JPedal、JODConverter 和阿里大鱼短信旧 SDK 相关能力。
- 新增最小初始化库 `database/bpmt-db.sql`。
- `docker-compose.yml` 默认只保留必要环境变量，高级配置通过 `config/overrides/*.properties` 覆盖。
- 默认日志配置调整为 `log.encoding=utf8`、`log.level=debug`。

## 最小初始化库

`database/bpmt-db.sql` 的来源：

- 平台表结构：旧项目 `support/hbm2ddl` 生成的 MySQL DDL。
- Activiti 表结构：`activiti-engine-5.16.3.jar` 内置 MySQL DDL。
- Quartz 表结构：`com.riversoft:quartz-ddl:2.2.1` 中的 MySQL DDL。
- 初始化数据：`bpmt_init_data.xlsx`。

验收结果：

- 导入后 `kyq` 共 173 张表。
- Activiti 表 24 张。
- Quartz 表 11 张。
- 包含最小系统数据和 1 个历史初始化用户。

## 验收记录

2026-04-26 已在本地完成发布候选验收：

- Java 8 + public-only 临时 Maven settings + 空本地 Maven 仓库执行 `mvn -s <tmp-settings> -DskipTests compile` 通过。
- `scripts/build-image.sh` 构建 `ghcr.io/wodenwang/bpmt-lite:1.1.0` 通过。
- 使用 `database/bpmt-db.sql` 从零初始化 MariaDB 通过。
- `/` 返回 `HTTP/1.1 200 OK`。
- `/ueditor/` 返回 `HTTP/1.1 200 OK`。
- `config/overrides/page.properties` 覆盖生效。
- 容器内 `log.properties` 默认值为 `log.encoding=utf8`、`log.level=debug`。

## 已知取舍

- 默认发行不再包含 Office/PDF 转换相关历史依赖；PDF 文件本身的上传、下载和直接预览不受影响。
- 阿里大鱼短信旧 SDK 已裁剪，相关类保留兼容说明，未来如需恢复应重新设计短信实现。
- 最小初始化库不是完整历史业务库，只用于快速体验和本地验证。
