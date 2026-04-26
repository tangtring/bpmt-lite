# v1.1.0 极简 kyq 初始化库 spec

## 背景

`v1.0.0` 支持通过 `db/init/kyq.sql` 初始化完整业务数据库，但公开仓库不包含可分发 SQL。使用者如果没有历史 `kyq.sql`，只能启动空数据库，无法得到可体验的 BPMT 初始化状态。

`v1.1.0` 需要整理一份更小、更适合公开分发的初始化库，让使用者可以用 Docker compose 快速得到一个最小可访问环境。

本 spec 的实现来源调整为：

- 平台表结构借鉴旧项目 `/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk/support/hbm2ddl`，由 Hibernate HBM 映射生成 MySQL DDL。
- Activiti 和 Quartz 这类框架表不由项目 HBM 生成，需从对应框架 SQL 来源补齐。
- 最小初始化数据以旧项目 `/Users/wenzhewang/workspace/bpmt_project/riversoft/package/database/bpmt_init_data.xlsx` 为准。
- 当前本地 `db/init/kyq.sql` 保留，不作为清洗输入，不做任何修改。

## 目标

- 交付一份可公开提交的最小 `bpmt-db.sql`。
- 初始化后平台首页 `/` 返回 200，`/ueditor/` 返回 200。
- 初始化库包含 HBM 平台表结构、Activiti 表结构、Quartz 表结构，以及 `bpmt_init_data.xlsx` 中定义的最小系统数据。
- 不从完整历史 `kyq.sql` 中抽取或清洗数据。
- 形成可重复的生成脚本，避免手工不可追溯。

## 非目标

- 不迁移或改造表结构。
- 不把完整历史业务库公开分发。
- 不修改 `db/init/kyq.sql`。
- 不修复历史测试数据质量问题。
- 不保证所有业务流程、报表、动态表单样例都可用。
- 不在本 spec 中调整 Docker compose 配置结构。

## 数据来源

表结构来源：

```text
/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk/support/hbm2ddl
```

`hbm2ddl` 的运行方式：

```bash
cd /Users/wenzhewang/workspace/bpmt_project/riversoft/trunk
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.xml -pl support/hbm2ddl -am -DskipTests dependency:build-classpath -Dmdep.outputFile=/tmp/hbm2ddl.classpath
CP="support/hbm2ddl/target/classes:util/target/classes:platform/target/classes:$(cat /tmp/hbm2ddl.classpath)"
java -Dfile.encoding=UTF-8 -cp "$CP" com.riversoft.hbm2ddl.Main \
  /Users/wenzhewang/workspace/bpmt_project/riversoft/trunk/support/hbm2ddl \
  target/hbm \
  target/sql-bpmt-lite
```

本轮已确认 `target/sql-bpmt-lite/mysql/create_model.sql` 可生成。

框架表结构来源：

```text
/Volumes/vm/maven/repository/org/activiti/activiti-engine/5.16.3/activiti-engine-5.16.3.jar
/Volumes/vm/maven/repository/com/riversoft/quartz-ddl/2.2.1/quartz-ddl-2.2.1.zip
```

其中：

- Activiti 使用 jar 内置的 `org/activiti/db/create/activiti.mysql.create.engine.sql`、`history.sql`、`identity.sql`。
- Quartz `quartz-2.2.1.jar` 本体和 sources jar 不包含 DDL；旧项目通过 `com.riversoft:quartz-ddl:2.2.1` 提供 `quartz-mysql-create.sql`。

初始化数据来源：

```text
/Users/wenzhewang/workspace/bpmt_project/riversoft/package/database/bpmt_init_data.xlsx
```

当前 Excel 包含以下 sheet：

- `CM_BASE_CATELOG`
- `CM_BASE_DATA`
- `CM_BASE_TYPE`
- `CM_DOMAIN`
- `CM_MENU`
- `CM_PRI`
- `US_USER`
- `US_GROUP`
- `US_ROLE`
- `US_GROUP_ROLE`
- `US_USER_GROUP_ROLE`

## 交付物

交付结构：

```text
database/bpmt-db.sql
database/README.md
scripts/build-minimal-bpmt-db.py
docs/v1.1.0/spec-minimal-kyq.md
```

`database/bpmt-db.sql` 不放入 `db/init/`，避免覆盖或混淆本地完整库 `db/init/kyq.sql`。用户需要使用最小库时，可以显式复制到运行目录的 `db/init/kyq.sql`。

## 验收标准

使用全新运行目录验证：

1. 删除或换用全新的 `db/data`。
2. 将 `database/bpmt-db.sql` 复制为运行目录中的 `db/init/kyq.sql`。
3. 执行 `docker compose up -d`。
4. MariaDB 健康检查通过。
5. 表数量记录到发布文档中。
6. `/` 返回 200。
7. `/ueditor/` 返回 200。
8. 若包含默认登录账号，必须记录账号来源和初始化方式；若不包含默认账号，README 必须明确说明。

验收命令示例：

```bash
docker compose down
rm -rf db/data
cp database/bpmt-db.sql db/init/kyq.sql
docker compose up -d
docker compose ps
docker compose exec -T mariadb mariadb -uroot -p123456 -N \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='kyq';"
curl -fsSI http://127.0.0.1:8080/
curl -fsSI http://127.0.0.1:8080/ueditor/
```

## 风险和决策点

- `bpmt_init_data.xlsx` 中包含默认 `admin` 用户，密码哈希为历史初始化数据。README 必须明确该账号仅用于本地体验，生产环境必须修改。
- HBM 生成的表结构只有核心平台表，不包含 Activiti/Quartz 这类框架表；生成脚本已显式追加这两类框架表。
- 最小库仍不包含历史完整库中的所有业务表；这符合最小化目标，但需要通过 Web 启动验证确认不会缺表。
- `hbm2ddl` 生成的是大写表名；Docker compose 当前 MariaDB 使用 `lower_case_table_names=1`，正式验证应以 compose 路径为准。
- 如果后续需要加入更多初始化数据，应优先修改或替换 Excel 来源，而不是从 `kyq.sql` 手工抽取。

## 实施顺序

1. 跑通旧项目 `hbm2ddl`，生成 MySQL `create_model.sql`。
2. 从 Activiti/Quartz 的框架 SQL 来源补齐 `ACT_*`、`QRTZ_*` 表结构。
3. 解析 `bpmt_init_data.xlsx`，将各 sheet 转换为对应表的 `INSERT`。
4. 合并表结构和初始化数据，生成 `database/bpmt-db.sql`。
5. 使用临时 MariaDB 容器验证 SQL 可导入。
6. 使用 Docker compose 从零导入验证 `/` 和 `/ueditor/`。
7. 更新 README 和维护文档，说明 `bpmt-db.sql` 与 `kyq.sql` 的区别。

## 当前实现记录

已新增：

- `scripts/build-minimal-bpmt-db.py`
- `database/bpmt-db.sql`
- `database/README.md`

当前 `database/bpmt-db.sql` 生成结果：

- SQL 大小约 132KB。
- 包含 173 张表结构，其中 Activiti 24 张、Quartz 11 张。
- 包含 107 条 `INSERT` 初始化数据。
- 临时 MariaDB 10.11 容器导入通过。
- 临时 compose 验证通过：`/` 返回 200，`/ueditor/` 返回 200。

本轮未修改 `db/init/kyq.sql`。
