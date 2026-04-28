# v1.2.0 发布验收清单

本清单用于 `v1.2.0` 打 tag 和发布镜像前的最终 gate。

## 静态检查

```bash
scripts/verify-repo.sh
docker compose config
```

期望：

- 仓库 hygiene 检查通过。
- Compose 配置可解析。
- `.vscode/`、`settings.local.xml`、`db/init/*.sql`、`db/data/`、`runtime/` 不进入提交。

## Java 编译

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -DskipTests compile
```

期望：`BUILD SUCCESS`。

## 数据库初始化

最小库：

```bash
scripts/init-db.sh min
```

期望：

- 生成或下载 `db/init/bpmt-min.sql`。
- 首次初始化后 MariaDB 中存在 `bpmt_min`。
- `bpmt_min` 表数量为 173。

完整库：

```bash
scripts/init-db.sh
```

期望：

- 生成或下载 `db/init/bpmt.sql`。
- 首次初始化后 MariaDB 中存在 `bpmt`。

当前阻塞：公开 `database/bpmt.sql` 需要先确认完整 `kyq` 数据源可以公开发布，并确认大 SQL 的仓库交付方式。

## Web 运行

默认库：

```bash
docker compose up -d
curl -fsSI http://127.0.0.1:8080/
curl -fsSI http://127.0.0.1:8080/ueditor/
```

最小库：

```bash
DB_NAME=bpmt_min docker compose up -d web
curl -fsSI http://127.0.0.1:8080/
curl -fsSI http://127.0.0.1:8080/ueditor/
```

期望：两个入口均返回 `HTTP/1.1 200`。

## 工作流设计器

从工作流设计入口打开 editor。

期望：

- 不再出现 `HTTP Status 404 - /service/editor`。
- editor 页面可加载模型。
- 保存请求返回 200。
- 关闭页面可访问。

## 品牌信息

检查登录页和主框架页。

期望：

- 默认 logo 显示 `BPMT`。
- 默认 copyright 不包含 `Riversoft Designs`。
- Java 包名、Maven groupId、历史源码路径不在本版本中重命名。
