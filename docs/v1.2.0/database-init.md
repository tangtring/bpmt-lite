# v1.2.0 数据库初始化设计

`v1.2.0` 开始把初始化数据库拆成两种公开选择：

| 模式 | 数据库 | SQL | 适用场景 |
| --- | --- | --- | --- |
| 默认 | `bpmt` | `database/bpmt.sql` | 使用完整 `kyq` 数据源整理出的本地试运行库。 |
| `min` | `bpmt_min` | `database/bpmt-min.sql` | 快速体验、自动化验收、issue 复现。 |

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

脚本优先从本地 `database/` 目录复制同名 SQL；如果本地不存在，则从 `BPMT_SQL_BASE_URL` 下载。默认下载地址指向：

```text
https://raw.githubusercontent.com/wodenwang/bpmt-lite/v1.2.0/database
```

本地测试未发布 tag 时，可以覆盖：

```bash
BPMT_SQL_BASE_URL=https://raw.githubusercontent.com/wodenwang/bpmt-lite/main/database scripts/init-db.sh min
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

当前本地存在历史完整 `db/init/kyq.sql`，但该文件是忽略文件，不直接提交。它体积约 173MB，且可能包含历史业务数据。生成公开的 `database/bpmt.sql` 前需要先确认两件事：

- 数据内容允许公开发布。
- 文件体积符合 GitHub 仓库限制，必要时需要清洗、压缩或拆分交付方案。

在这两个条件确认前，仓库只提交 `bpmt-min.sql` 和初始化脚本能力，不把本地完整 `kyq.sql` 转入 git。
