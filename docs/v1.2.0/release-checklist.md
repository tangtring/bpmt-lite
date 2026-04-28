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

## Docker 镜像

```bash
scripts/build-image.sh
docker run --rm --entrypoint sh ghcr.io/wodenwang/bpmt-lite:<tag> -lc 'fc-match "WenQuanYi Zen Hei" && fc-list :lang=zh | head'
```

期望：

- 镜像使用当前机器原生架构构建，不再强制 `linux/amd64`。
- 镜像中存在 `ROOT`、`ueditor` 和 `/usr/local/bin/bpmt-entrypoint.sh`。
- 镜像中存在 CJK 字体，流程图节点中文不再显示为方框。

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
- 仓库提交的是 `database/bpmt.sql.gz`，脚本会自动解压。

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
- 审批流流程图可显示，节点中文可读。

注意：如果某些流程定义返回的是部署时缓存的旧 PNG，安装字体后不会自动重绘旧缓存图；验收中文字体时应至少覆盖一条运行时重新生成的流程图，或重新部署流程定义后再检查。

## 品牌信息

检查登录页和主框架页。

期望：

- 默认 logo 显示 `BPMT`。
- 默认 copyright 不包含 `Riversoft Designs`。
- Java 包名、Maven groupId、历史源码路径不在本版本中重命名。
