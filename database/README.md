# bpmt-lite 初始化数据库

本目录用于放置可公开分发的初始化 SQL。

## v1.2.0 约定

- `bpmt-min.sql` 是最小初始化库，数据库名为 `bpmt_min`。
- `bpmt.sql` 是完整初始化库，数据库名为 `bpmt`。
- 两份 SQL 都必须自己包含 `CREATE DATABASE IF NOT EXISTS ...` 和 `USE ...`，不能依赖 Docker Compose 的 `MARIADB_DATABASE` 自动建库行为。
- `db/init/*.sql` 是本地运行目录，不提交 git。

当前已提交的是 `bpmt-min.sql`。完整 `bpmt.sql` 需要从可公开分发的 `kyq` 数据源整理后再放入本目录；不要直接提交本地私有 `db/init/kyq.sql`。

## 最小库来源

`bpmt-min.sql` 继承 `v1.1.0` 的最小初始化库：

- 平台表结构来自旧项目 `support/hbm2ddl` 生成的 MySQL DDL。
- Activiti 表结构来自 `activiti-engine-5.16.3.jar` 内置的 MySQL DDL。
- Quartz 表结构来自 `com.riversoft:quartz-ddl:2.2.1` 中的 MySQL DDL。
- 初始化数据来自旧项目 `package/database/bpmt_init_data.xlsx`。

已验证结果：

- 导入后 `bpmt_min` 包含 173 张表，其中 Activiti 24 张、Quartz 11 张。
- 最小初始化数据包含 1 个用户、26 个菜单、27 条权限和 1 条用户角色关系。

## 重新生成最小库

```bash
scripts/build-minimal-bpmt-db.py
```

如果 hbm2ddl 输出目录不存在，先在旧项目中生成 MySQL DDL：

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
