# AGENTS.md

## 作用范围

本文件是 `bpmt-lite` 仓库的本地协作与交接文档。
后续 Codex agent 在本仓处理环境、编译、打包、Docker 运行、配置覆盖、文档更新时，必须先读本文件，再做判断。

## 项目定位

- 仓库：`bpmt-lite`
- 目标：对遗留 BPMT 平台做简化发行工程
- 核心原则：只调整代码结构、打包方式、配置方式、部署方式
- 明确边界：不升级技术栈、不重写功能、不额外增加功能
- 运行栈：Java 8、Maven 3、Tomcat 7、MariaDB

## README 约定

- `v1.0.0` 是首个正式 Docker 化版本。
- `v1.1.0` 是已发布的第二个 Docker 化版本。
- `v1.2.0` 是当前规划中的下一个版本。
- 默认 Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.1.0`
- 同步镜像 tag：发布后同步到 `ghcr.io/wodenwang/bpmt-lite:latest`
- 默认访问地址：`http://127.0.0.1:8080/`
- `ROOT` 应用对应 BPMT `platform`
- 额外包含 `/ueditor` 应用
- MariaDB 初始化数据库名：`kyq`
- README 中记录的发布验收基线是：
  - 使用 `database/bpmt-db.sql` 最小库初始化后 173 张表
  - `/` 返回 200
  - `/ueditor/` 返回 200

## 文档与沟通规则

- 本项目沟通和文档统一使用简体中文。
- 代码、命令、配置键名、Maven 坐标、镜像名等技术标识保持原样。
- 如果后续 agent 更新运行说明、维护说明或交接说明，应与 README 的中文风格保持一致。
- v1.2.0 期间的 source-of-truth 顺序是：`AGENTS.md` -> `docs/v1.2.0/*` -> `README.md` -> implementation。
- 涉及 Docker、数据库、初始化脚本、发布验收、公开文档的变更，必须同步更新对应文档，不能只改代码。

## 已验证的本地编译基线

2026-04-26 在当前 checkout 已验证：

- JDK：`/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home`
- Maven 本地仓库：`/Volumes/vm/maven/repository`
- Maven settings：`settings.local.xml`
- 全仓编译命令：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -DskipTests compile
```

验证结果：

- Reactor 模块：`riversoft-product`、`parent`、`util`、`magic`、`magic-api`、`magic-api-impl`、`dbtools`、`platform`
- 结果：`BUILD SUCCESS`

## 不可变的环境规则

- 本仓所有构建、依赖导入、IDE Java language server 都必须使用 Java 8。
- 不要假设本仓 Maven 使用 `~/.m2/repository`。
- 当前机器在本仓应使用的 Maven 本地仓库路径是 `/Volumes/vm/maven/repository`。
- 优先使用 `mvn -s settings.local.xml ...`，不要默认裸跑 `mvn`。
- 如果 VS Code 出现大量 Java 编译错误，先检查：
  - 工作区 JDK 是否为 Java 8
  - Maven settings 是否指向 `settings.local.xml`
  - `/Volumes/vm/maven/repository` 是否可读

## Maven 配置规则

- `settings.local.xml` 是当前 checkout 的有效本地配置。
- `settings.example.xml` 是公开示例配置，不能写死本机 Maven 本地仓库路径。
- 已退役的 RiverSoft 私有仓库地址不得重新引入：
  - `https://nexus.riversoft.com.cn/repository/maven-public/`
  - `https://nexus.riversoft.com.cn/repository/Riversoft-release/`
  - `https://nexus.riversoft.com.cn/repository/Riversoft-snapshot/`
- 当前仓库策略：
  - `settings.example.xml` 使用 Maven 默认本地仓库
  - 本机 `settings.local.xml` 可以继续使用 `/Volumes/vm/maven/repository`
  - 公共镜像：Aliyun mirror of Central
  - Central：兜底仓库定义

## VS Code / IDE 规则

当前机器可以继续保留本地 VS Code 设置，但 `.vscode/` 从 v1.2.0 起视为本地 IDE 配置，不再提交 GitHub。

如果需要在本机恢复 VS Code 配置，规则仍然是：

- Java runtime 固定为 Java 8
- Maven / Java import 使用 `settings.local.xml`

如果 IDE 仍显示旧错误，按以下顺序处理：

1. 执行 `Java: Clean Java Language Server Workspace`
2. Reload VS Code 窗口
3. 重新触发 Maven project import

## Docker 与运行约定

- 默认启动方式是 `docker compose up -d`。
- 快速体验允许只拉起容器而不导入业务数据。
- 若要得到完整初始化业务数据，必须提供 `db/init/kyq.sql`。
- MariaDB 只会在首次创建 `db/data` 时自动导入 `db/init/kyq.sql`。
- 如果已经启动过，再替换 `kyq.sql` 不会自动重新导入。
- 需要重新初始化数据库时，先确认数据已备份，再执行：

```bash
docker compose down
rm -rf db/data
docker compose up -d
```

## 默认访问与常用配置

- 平台入口：`http://127.0.0.1:8080/`
- UEditor：`http://127.0.0.1:8080/ueditor/`
- 常用环境变量：
  - `BPMT_HTTP_PORT`
  - `BPMT_DB_PORT`
  - `BPMT_IMAGE_TAG`
  - `DB_HOST`
  - `DB_NAME`
  - `DB_USER`
  - `DB_PASSWORD`

后续 agent 如果修改 `docker-compose.yml`、镜像构建脚本或 README，不能破坏这些默认约定，除非用户明确要求变更。

## 运行目录约定

以下目录是 README 明确约定的运行目录，后续 agent 不应随意改语义：

- `db/init/kyq.sql`
  - 首次初始化数据库备份
  - 不提交 git
- `db/data/`
  - MariaDB 数据目录
  - 不提交 git
- `db/logs/`
  - MariaDB 日志目录
  - 不提交 git
- `runtime/attachment/`
  - BPMT 附件目录
  - 不提交 git
- `runtime/download/`
  - BPMT 下载目录
  - 不提交 git
- `runtime/ueditor-upload/`
  - UEditor 上传目录
  - 不提交 git
- `runtime/platform-logs/`
  - 平台日志目录
  - 不提交 git
- `runtime/tomcat-logs/`
  - Tomcat 日志目录
  - 不提交 git
- `config/overrides/`
  - properties 覆盖目录
  - 不提交具体覆盖文件

额外规则：

- `config/overrides/*.properties` 会追加到容器启动时生成的同名 properties 文件之后。
- 覆盖文件中的同名 key 优先级更高。

## 维护者构建约定

README 中的维护者构建入口是：

```bash
cp settings.example.xml settings.local.xml
scripts/build-image.sh
```

维护相关约束：

- 维护者需要 Java 8、Maven、Docker，以及可访问历史依赖的 Maven 仓库。
- `settings.local.xml` 是本地文件，不应提交到 git。
- 更多维护和发布细节见 `docs/maintenance.md`。

## 构建与排障顺序

遇到构建或导入异常时，按下面顺序排查：

1. 确认 `JAVA_HOME` 指向上面的 Java 8 JDK
2. 确认 Maven 使用 `-s settings.local.xml`
3. 确认 `/Volumes/vm/maven/repository` 已挂载且可读
4. 先跑窄范围模块，再跑全仓

常用验证命令：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
mvn -s settings.local.xml -pl dbtools -am -DskipTests compile
mvn -s settings.local.xml -DskipTests compile
docker compose ps
```

## 当前状态

截至 2026-04-26，本仓当前状态：

- VS Code 工作区已修正为 Java 8 + repo-local Maven settings
- `settings.local.xml` 与 `settings.example.xml` 已对齐到 `/Volumes/vm/maven/repository`
- 根 `pom.xml` 中已移除退役私服和旧的 distribution-management 引用
- 全仓 `mvn -s settings.local.xml -DskipTests compile` 已验证成功
- Maven 项目版本已切到 `1.1.0`
- 默认 `docker-compose.yml` 镜像 tag 已切到 `1.1.0`
- `database/bpmt-db.sql` 是 v1.1.0 最小初始化库，导入后 173 张表，其中 Activiti 24 张、Quartz 11 张
- `docker-compose.yml` 已完成配置瘦身，高级配置继续通过 `config/overrides/*.properties` 覆盖
- `scripts/build-image.sh` 已验证可构建本地镜像 `ghcr.io/wodenwang/bpmt-lite:1.1.0`
- 使用 `database/bpmt-db.sql` + 本地 `1.1.0` 镜像的临时 compose 验证通过：`/` 和 `/ueditor/` 均返回 200
- 使用 public-only 临时 Maven settings + 空本地 Maven 仓库执行 `mvn -s <tmp-settings> -DskipTests compile` 已验证成功

## v1.2.0 当前规划

截至 2026-04-28，v1.2.0 已进入执行阶段，当前状态如下：

- 第一阶段修复 GitHub issue 已在本地提交：
  - `#6`：已清理 `settings.example.xml` 中的本机 Maven 仓库路径和非必要镜像配置
  - `#7`：已通过 `ModelerServiceServlet` 恢复 Activiti Modeler `/service/*` 兼容端点，并验证 editor 打开、保存、关闭路径
- Docker 运行问题已推进：
  - Web 镜像构建不再强制 `linux/amd64`，本机 Apple Silicon 构建结果为 `linux/arm64`
  - `docker/Dockerfile` 改为 `eclipse-temurin:8-jdk-jammy` 并手动安装 Tomcat 7.0.109
  - 镜像内安装 `fonts-wqy-zenhei`，`activiti.font` 默认改为 `WenQuanYi Zen Hei`
  - `scripts/build-image.sh` 构建后会启动临时容器验证 `ROOT`、`ueditor`、entrypoint 和 CJK 字体
  - 已用容器内 Java2D 和 Activiti `DefaultProcessDiagramGenerator` 生成临时 PNG，确认中文可读；旧部署缓存 PNG 不会自动重绘
- 第二阶段整理初始化数据库正在推进：
  - `bpmt` 使用完整 `kyq` 数据源整理出的初始化 SQL
  - `bpmt_min` 使用最小初始化 SQL
  - 两个 database 允许在同一个 MariaDB 实例中共存
  - 默认初始化脚本导入 `bpmt`，参数 `min` 导入 `bpmt_min`
  - 本地完整 `db/init/kyq.sql` 约 173MB 且不提交；生成公开 `database/bpmt.sql` 前必须确认数据可公开和文件体积交付方案
- 第三阶段重构 README，使初学者优先看到 Docker 一条命令启动、默认账号密码、数据库选择和切换方式。
- 第四阶段补齐团队开发模式：
  - 每个阶段要有可验证结果
  - 大改前先写 `docs/v1.2.0/*`
  - 需要 reviewer gate 时，先用文档清单审查再收口
- 第五阶段清理品牌信息：
  - 默认 logo 替换为简约 `BPMT` 字样
  - 默认 copyright 去掉 `Riversoft Designs`
  - 未来许可证考虑 MIT，主要作者为 `wodenwang` 和 `borballzhai`

v1.2.0 规划文档见：

- `docs/v1.2.0/roadmap.md`
- `docs/superpowers/plans/2026-04-28-bpmt-lite-v1.2.0.md`

## 后续 agent 编辑规则

- 保持 Java 8 兼容性。
- 未经用户明确要求，不做技术栈升级。
- 不要凭印象恢复已退役私服配置。
- 涉及运行、打包、初始化数据库、目录语义时，先以 README 和本文件为准。
- 如果用户要求记录当前阶段、当前环境或交接状态，优先更新本文件。
