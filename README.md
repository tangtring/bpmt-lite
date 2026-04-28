# bpmt-lite

`bpmt-lite` 是 BPMT 低代码平台的简化发行工程。BPMT 表示 BPM + table，核心能力是自定义工作流和动态表格。

本项目只整理发行工程：代码结构、打包方式、配置方式、Docker 运行方式和初始化数据。不升级 Java/Tomcat/MariaDB 技术栈，不重写业务功能。

## 当前版本

`v1.2.0` 正在落地中，目标是修复 `v1.1.0` 后发现的问题，并把初始化数据库、文档、品牌信息和 agent 交接方式整理清楚。

- 默认 Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.1.0`
- 默认访问地址：`http://127.0.0.1:8080/`
- Web 应用：Tomcat `ROOT`
- 附带应用：`/ueditor`
- 默认数据库名：`bpmt`
- 最小数据库名：`bpmt_min`
- 默认登录账号：`admin/admin`

## 最快启动

只想先把系统跑起来，不需要 clone 项目。当前公开仓库已经包含最小初始化库，推荐先使用最小库启动：

```bash
mkdir -p bpmt-lite && cd bpmt-lite && curl -fsSL https://raw.githubusercontent.com/wodenwang/bpmt-lite/v1.2.0/scripts/run.sh -o run.sh && sh run.sh min
```

访问：

```text
http://127.0.0.1:8080/
```

登录：

```text
用户名：admin
密码：admin
```

最小库包含 173 张表和最小系统数据，适合快速体验、自动化验收和 issue 复现。

## 完整库启动

`v1.2.0` 提供完整初始化库压缩包 `database/bpmt.sql.gz`，数据库名为 `bpmt`。初始化脚本会自动解压到 `db/init/bpmt.sql`。

完整库一条命令：

```bash
mkdir -p bpmt-lite && cd bpmt-lite && curl -fsSL https://raw.githubusercontent.com/wodenwang/bpmt-lite/v1.2.0/scripts/run.sh -o run.sh && sh run.sh
```

如果你本机已有可导入的完整 SQL，也可以直接放到：

```text
bpmt-lite/db/init/bpmt.sql
```

然后启动：

```bash
docker compose up -d
```

## 数据库选择

`bpmt` 和 `bpmt_min` 可以共存在同一个 MariaDB 容器里，互不覆盖。

| 数据库 | SQL 文件 | 用途 |
| --- | --- | --- |
| `bpmt` | `db/init/bpmt.sql` | 完整业务数据，本地试运行，由 `database/bpmt.sql.gz` 解压生成 |
| `bpmt_min` | `db/init/bpmt-min.sql` | 最小数据，快速体验和验收，由 `database/bpmt-min.sql.gz` 解压生成 |

Web 应用连接哪个库由 `DB_NAME` 决定。

切到最小库：

```bash
DB_NAME=bpmt_min docker compose up -d web
```

切回默认完整库：

```bash
DB_NAME=bpmt docker compose up -d web
```

注意：MariaDB 官方镜像只会在首次创建 `db/data` 时自动执行 `db/init/*.sql`。如果已经启动过，再新增或替换 SQL 文件不会自动重新导入。

## 常用操作

查看容器状态：

```bash
docker compose ps
```

停止服务：

```bash
docker compose down
```

重新初始化数据库前先确认数据已备份，然后删除本地数据目录：

```bash
docker compose down
rm -rf db/data
docker compose up -d
```

检查入口：

```bash
curl -fsSI http://127.0.0.1:8080/
curl -fsSI http://127.0.0.1:8080/ueditor/
```

期望返回 `HTTP/1.1 200`。

## 常用配置

默认 `docker-compose.yml` 只保留快速启动需要的常用项。

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `BPMT_HTTP_PORT` | `8080` | Web 访问端口 |
| `BPMT_DB_PORT` | `3306` | MariaDB 暴露到宿主机的端口 |
| `BPMT_IMAGE_TAG` | `1.1.0` | Web 镜像 tag |
| `DB_HOST` | `mariadb` | Web 容器访问数据库的主机名 |
| `DB_NAME` | `bpmt` | Web 应用连接的数据库 |
| `DB_USER` | `root` | 数据库用户 |
| `DB_PASSWORD` | `123456` | 数据库密码 |

示例：把 Web 端口改成 18080。

```bash
BPMT_HTTP_PORT=18080 docker compose up -d
```

高级配置通过 `config/overrides/*.properties` 覆盖。覆盖文件会追加到容器启动时生成的同名 properties 文件后面，因此同名 key 以覆盖文件为准。

## 运行目录

```text
db/init/                 初始化 SQL 目录，不提交私有 SQL
db/data/                 MariaDB 数据目录，不提交 git
db/logs/                 MariaDB 日志目录，不提交 git
runtime/attachment/      BPMT 附件目录，不提交 git
runtime/download/        BPMT 下载目录，不提交 git
runtime/ueditor-upload/  UEditor 上传目录，不提交 git
runtime/platform-logs/   BPMT 平台日志目录，不提交 git
runtime/tomcat-logs/     Tomcat 日志目录，不提交 git
config/overrides/        properties 覆盖文件目录，不提交具体覆盖文件
```

## 维护者构建

维护者需要 Java 8、Maven、Docker，以及可访问历史依赖的 Maven 仓库。

```bash
cp settings.example.xml settings.local.xml
scripts/build-image.sh
```

`scripts/build-image.sh` 会构建本地镜像，并验证 `ROOT`、`ueditor`、entrypoint 和 CJK 字体。更多维护和发布细节见 [docs/maintenance.md](docs/maintenance.md)。

## 文档

- 初始化数据库设计：[docs/v1.2.0/database-init.md](docs/v1.2.0/database-init.md)
- v1.2.0 规划：[docs/v1.2.0/roadmap.md](docs/v1.2.0/roadmap.md)
- 发布验收清单：[docs/v1.2.0/release-checklist.md](docs/v1.2.0/release-checklist.md)
- 维护说明：[docs/maintenance.md](docs/maintenance.md)

## 许可证与作者

未来版本计划使用 MIT 许可证。主要作者记录为 `wodenwang` 和 `borballzhai`。

本项目沟通和文档统一使用简体中文；代码、命令、配置键名、Maven 坐标、镜像名等技术标识保持原样。
