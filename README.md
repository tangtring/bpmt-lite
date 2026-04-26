# bpmt-lite

`bpmt-lite` 是 BPMT 低代码平台的简化发行工程。BPMT 表示 BPM + table，核心能力是自定义工作流和动态表格。

本项目只调整代码结构、打包方式、配置方式和部署方式，不升级技术栈、不重写功能、不增加功能。运行栈继续保持 Java 8、Tomcat 7、MariaDB。

## v1.0.0 状态

`v1.0.0` 是 bpmt-lite 的首个正式 Docker 化版本。

- GitHub Release: https://github.com/wodenwang/bpmt-lite/releases/tag/v1.0.0
- 默认 Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.0.0`
- 同步镜像 tag：`ghcr.io/wodenwang/bpmt-lite:latest`
- 默认 Web 访问地址：`http://127.0.0.1:8080/`
- Tomcat 中的 `ROOT` 应用是 BPMT `platform`
- Tomcat 中额外包含 `/ueditor` 应用
- MariaDB 初始化数据库名：`kyq`
- 发布验收：`kyq` 初始化后 383 张表，`/` 和 `/ueditor/` 均返回 200

## Quick Start

只想把系统跑起来，不需要 clone 本项目。任选下面一种方式。

### 一条命令启动

适合先快速体验容器是否能跑起来：

```bash
mkdir -p bpmt-lite && cd bpmt-lite && curl -fsSL https://raw.githubusercontent.com/wodenwang/bpmt-lite/v1.0.0/docker-compose.yml -o docker-compose.yml && docker compose up -d
```

然后访问：

```text
http://127.0.0.1:8080/
```

说明：这个命令不会初始化业务数据，因为公开仓库不包含 `kyq.sql`。

### 一条命令完整初始化

如果你本机已经有 `kyq.sql`，使用下面的命令。把 `/path/to/kyq.sql` 改成你的真实文件路径：

```bash
KYQ_SQL=/path/to/kyq.sql; mkdir -p bpmt-lite/db/init && cp "$KYQ_SQL" bpmt-lite/db/init/kyq.sql && cd bpmt-lite && curl -fsSL https://raw.githubusercontent.com/wodenwang/bpmt-lite/v1.0.0/docker-compose.yml -o docker-compose.yml && docker compose up -d
```

第一次启动会自动拉取镜像、启动 MariaDB、导入 `db/init/kyq.sql`。

### 常规方式启动

如果你已经 clone 了项目，或者已经有完整目录结构，将初始化数据库文件放到：

```text
db/init/kyq.sql
```

然后启动：

```bash
docker compose up -d
```

如果没有 `kyq.sql`，容器仍会启动，但不会得到完整的初始化业务数据。

### 查看状态

```bash
docker compose ps
```

看到 `bpmt-lite-mariadb` 是 `healthy`，`bpmt-lite-web` 是 `Up` 即可。

### 访问地址

平台入口：

```text
http://127.0.0.1:8080/
```

UEditor 应用：

```text
http://127.0.0.1:8080/ueditor/
```

### 停止

```bash
docker compose down
```

该命令只停止并删除容器，不会删除 `db/data`、`runtime` 下的数据文件。

## 重新初始化数据库

MariaDB 只会在首次创建 `db/data` 数据目录时自动导入 `db/init/kyq.sql`。

如果已经启动过，再替换 `kyq.sql` 不会自动重新导入。需要重新初始化时，先确认已经备份数据，然后删除本地数据目录：

```bash
docker compose down
rm -rf db/data
docker compose up -d
```

## 常用配置

默认 `docker-compose.yml` 只保留快速启动需要的常用项，可以直接通过环境变量覆盖。

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `BPMT_HTTP_PORT` | `8080` | Web 访问端口 |
| `BPMT_DB_PORT` | `3306` | MariaDB 暴露到宿主机的端口 |
| `BPMT_IMAGE_TAG` | `1.0.0` | Web 镜像 tag |
| `DB_HOST` | `mariadb` | Web 容器访问数据库的主机名 |
| `DB_NAME` | `kyq` | 数据库名 |
| `DB_USER` | `root` | 数据库用户 |
| `DB_PASSWORD` | `123456` | 数据库密码 |

示例：把 Web 端口改成 18080。

```bash
BPMT_HTTP_PORT=18080 docker compose up -d
```

高级配置不再展开在默认 compose 中。需要覆盖历史 `*.properties` 参数时，在 `config/overrides/` 下创建同名 properties 文件即可。该目录提供了几个示例：

```text
config/overrides/page.properties.example
config/overrides/log.properties.example
config/overrides/jdbc.properties.example
```

例如覆盖页面标题：

```bash
printf 'page.title=BPMT Lite\n' > config/overrides/page.properties
docker compose up -d
```

## 运行目录

运行时目录约定如下：

```text
db/init/kyq.sql          首次初始化数据库备份，不提交 git
db/data/                 MariaDB 数据目录，不提交 git
db/logs/                 MariaDB 日志目录，不提交 git
runtime/attachment/      BPMT 附件目录，不提交 git
runtime/download/        BPMT 下载目录，不提交 git
runtime/ueditor-upload/  UEditor 上传目录，不提交 git
runtime/platform-logs/   BPMT 平台日志目录，不提交 git
runtime/tomcat-logs/     Tomcat 日志目录，不提交 git
config/overrides/        properties 覆盖文件目录，不提交具体覆盖文件
```

`config/overrides/*.properties` 会追加到容器启动时生成的同名 properties 文件后面，因此覆盖文件中的同名 key 优先级更高。`.example` 文件只作为示例，不会被运行时读取。

## 维护者构建

维护者需要 Java 8、Maven、Docker，以及可访问旧私有依赖的 Maven 仓库。

```bash
cp settings.example.xml settings.local.xml
scripts/build-image.sh
```

`settings.local.xml` 不提交到 git。

更多维护和发布细节见 [docs/maintenance.md](docs/maintenance.md)。

## 项目语言

本项目沟通和文档统一使用简体中文。代码、命令、配置键名、Maven 坐标、镜像名等技术标识保持原样。
