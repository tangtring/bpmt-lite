# v1.2.0 数据库初始化设计

`v1.2.0` 开始把初始化数据库拆成两种公开选择：

| 模式 | 数据库 | SQL | 适用场景 |
| --- | --- | --- | --- |
| 默认 | `bpmt` | `database/bpmt.sql.gz` | 使用当前整理后的 `bpmt` 数据库导出的本地试运行库。 |
| `min` | `bpmt_min` | `database/bpmt-min.sql.gz` | 快速体验、自动化验收、issue 复现。 |

## 初始化脚本

仓库提供统一入口：

```bash
scripts/init-db.sh
scripts/init-db.sh min
```

默认模式会准备：

```text
db/init/bpmt.sql
```

`min` 模式会准备：

```text
db/init/bpmt-min.sql
```

脚本优先从本地 `database/` 目录复制同名 SQL；如果只有 `.sql.gz`，则自动解压到 `db/init/`。如果本地不存在，则从 `BPMT_SQL_BASE_URL` 下载。默认下载地址指向：

```text
https://raw.githubusercontent.com/wodenwang/bpmt-lite/v1.2.0/database
```

本地测试未发布 tag 时，可以覆盖：

```bash
BPMT_SQL_BASE_URL=https://raw.githubusercontent.com/wodenwang/bpmt-lite/main/database scripts/init-db.sh min
```

也可以使用一键运行脚本，它会下载 `docker-compose.yml`、下载初始化脚本、解压目标 SQL，并启动服务：

```bash
scripts/run.sh
scripts/run.sh min
```

## 共存方式

`bpmt` 和 `bpmt_min` 可以在同一个 MariaDB 实例中共存。关键约束是 SQL 文件必须自己选择数据库：

```sql
CREATE DATABASE IF NOT EXISTS `bpmt` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `bpmt`;
```

```sql
CREATE DATABASE IF NOT EXISTS `bpmt_min` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `bpmt_min`;
```

Docker Compose 中的 `DB_NAME` 只决定 Web 应用连接哪个数据库，不作为 SQL 文件的唯一建库依据。

## 切换运行库

默认连接 `bpmt`：

```bash
docker compose up -d
```

连接最小库：

```bash
DB_NAME=bpmt_min docker compose up -d web
```

如果 MariaDB 已经初始化过，新增 SQL 文件不会自动导入。需要导入新的 SQL 时，应先确认数据已备份，再重新创建数据库数据目录；或者手工进入 MariaDB 导入目标 SQL。

## 完整库交付状态

当前完整库以 `database/bpmt.sql.gz` 提交。原始 `database/bpmt.sql` 约 127 MiB，超过 GitHub 普通仓库 100 MiB 单文件限制，因此只保留本地生成文件，不直接提交。最小库也统一以 `database/bpmt-min.sql.gz` 提交，raw `database/bpmt-min.sql` 只作为本地生成文件保留。

本次导出排除了失效视图 `v_demo_qj`。该视图依赖的 demo 表已经不在当前 `bpmt` 数据库中，保留它会导致 `mariadb-dump` 中断，也不利于后续恢复初始化库。
