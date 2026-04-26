# bpmt-lite 最小数据库

本目录用于放置 `v1.1.0` 的最小化初始化数据库交付物。

当前约定：

- `bpmt-db.sql` 是新的最小化初始化 SQL。
- `db/init/kyq.sql` 是本地完整库备份入口，不在本任务中修改。
- 表结构来自旧项目 `support/hbm2ddl` 生成的 MySQL DDL。
- 初始化数据来自旧项目 `package/database/bpmt_init_data.xlsx`。

当前验证结果：

- `bpmt-db.sql` 可在临时 MariaDB 10.11 容器中导入。
- 导入后 `kyq` 包含 138 张表。
- 最小初始化数据包含 1 个用户、26 个菜单、27 条权限和 1 条用户角色关系。
- 使用临时 compose 目录、`ghcr.io/wodenwang/bpmt-lite:1.0.0`、`bpmt-db.sql` 作为 `db/init/kyq.sql` 验证，`/` 和 `/ueditor/` 均返回 200。

重新生成：

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
