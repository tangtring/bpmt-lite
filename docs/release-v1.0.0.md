# bpmt-lite v1.0.0 发布说明

`v1.0.0` 是 bpmt-lite 的首个正式版本，目标是把原 BPMT 核心 Web 服务整理为更简洁的 Docker 化发行工程。

## 发布结论

- Git tag: `v1.0.0`
- GitHub Release: https://github.com/wodenwang/bpmt-lite/releases/tag/v1.0.0
- 默认 Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.0.0`
- 同步镜像：`ghcr.io/wodenwang/bpmt-lite:latest`
- 数据库镜像：`mariadb:10.11`
- 默认数据库名：`kyq`
- 默认 Web 端口：`8080`
- 默认数据库端口：`3306`

## 范围

本版本只做工程结构和发行方式收口：

- 保留 Java 8、Tomcat 7、MariaDB 技术栈
- 保留原 `platform` 核心 Web 功能
- Tomcat `ROOT` 应用为 `platform`
- Tomcat 额外部署 `/ueditor` 应用
- 使用 `docker compose` 管理 Web 和 MariaDB 两个实例
- 使用宿主机目录挂载附件、下载文件、UEditor 上传文件、平台日志、Tomcat 日志、MariaDB 数据和 MariaDB 日志
- 通过 compose 环境变量暴露原 properties 中的主要配置项

本版本不包含：

- Java、Spring、Tomcat、MariaDB 技术升级
- 业务功能重写
- 新功能扩展
- 私有旧依赖的替换方案

## 快速启动

把初始化 SQL 放到：

```text
db/init/kyq.sql
```

启动：

```bash
docker compose up -d
```

访问：

```text
http://127.0.0.1:8080/
```

UEditor：

```text
http://127.0.0.1:8080/ueditor/
```

## 运行目录

| 目录 | 用途 |
| --- | --- |
| `db/init/kyq.sql` | 首次初始化数据库备份 |
| `db/data/` | MariaDB 数据目录 |
| `db/logs/` | MariaDB 日志目录 |
| `runtime/attachment/` | BPMT 附件目录 |
| `runtime/download/` | BPMT 下载目录 |
| `runtime/ueditor-upload/` | UEditor 上传目录 |
| `runtime/platform-logs/` | BPMT 平台日志目录 |
| `runtime/tomcat-logs/` | Tomcat 日志目录 |
| `config/overrides/` | properties 覆盖文件目录 |

## 验收结果

发布前已完成以下验证：

- `scripts/verify-repo.sh` 通过
- Java 8 Maven 编译通过
- `scripts/build-image.sh` 本地构建通过
- 匿名拉取 `ghcr.io/wodenwang/bpmt-lite:1.0.0` 通过
- 匿名拉取 `ghcr.io/wodenwang/bpmt-lite:latest` 通过
- 基于 `v1.0.0` tag 干净克隆启动通过
- `kyq` 初始化后表数量为 `383`
- `/` 返回 `200`
- `/ueditor/` 返回 `200`

## 已知限制

- `kyq.sql` 不提交到 git，需要部署者自行放入 `db/init/kyq.sql`
- MariaDB 只会在首次创建 `db/data` 时导入 `db/init/kyq.sql`
- 如果已经初始化过数据库，替换 `kyq.sql` 不会自动重新导入
- Web 镜像仍基于 Tomcat 7 和 Java 8，属于兼容性保留，不代表推荐新项目继续使用该技术栈
- 项目仍依赖部分旧私有 Maven 包，维护者构建时需要可访问旧依赖仓库
- `1.0.0` 和 `latest` 镜像 tag 指向已验证 digest：`sha256:8d3071c2b43e472beb0f453990b95c057895bf02bdd4be0dcdf74e7b336ba961`

## GHCR 说明

正式收口时，本机到 GHCR 的大层上传链路多次断流。最终发布采用 registry 级别重标记：将已公开、已完整验证的镜像 digest 发布为 `1.0.0` 和 `latest`。

该处理不改变运行内容。发布前已对镜像运行内容和 compose 启动结果做验收。
