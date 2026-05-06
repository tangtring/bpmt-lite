# v1.6.2 发布记录

## 版本定位

`v1.6.2` 是基于 `v1.6.1` 的补丁版本，目标是修复 GitHub 当前两个 open issue，并把安装、升级和 README 入口整理成面向使用者的稳定路径。

## 主要变化

- 修复 issue #13：微信/企业微信登录后若当前 BPMT 用户没有目标第三方系统权限，页面和回调错误信息改为明确提示 `用户[xxx]不具备访问本应用权限。`。
- 修复 issue #12：第三方系统管理表单中的微信类型和服务号 Scope 改为平台统一 `select` widget，并在微信配置后增加“访问控制”分组，避免下面的状态、权限和说明被误解为微信登录配置。
- 重构 `scripts/install.sh`：默认从零安装完整库，创建运行目录，并带出 `run.sh`、`upgrade.sh`。
- 新增 `scripts/upgrade.sh`：默认跟随 GitHub 最新 release/tag，拉取 BPMT Web/API `latest` 镜像，执行版本间 SQL 升级脚本，下载目标版本 compose 参考文件，不覆盖当前 `docker-compose.yml`。
- 新增 `.bpmt-lite/` 运行状态约定：安装和升级状态记录在项目运行目录内，不写入业务数据库。
- 重构 README：按项目介绍、Quick Start、文件结构、版本历史、文档、许可证与作者重新组织。
- 确认 MIT 许可证，作者为 `wodenwang` 和 `borball`。

## 升级说明

已有运行目录中执行：

```bash
cd bpmt-lite
sh ./upgrade.sh
```

`upgrade.sh` 默认只处理 BPMT Web/API 镜像和版本间 SQL。`mariadb`、`nginx` 等第三方容器不会被自动拉取或升级。

`v1.6.1` 到 `v1.6.2` 没有业务数据库结构变更，因此 `database/upgrade/manifest.txt` 中没有需要执行的 SQL 步骤。

## 验收命令

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -pl platform -Dtest=OAuthActionTest test
mvn -s settings.local.xml -pl platform -Dtest=ThirdpartJspTest,ThirdpartActionTest,ThirdpartServiceTest test
mvn -s settings.local.xml -DskipTests compile
mvn -s settings.local.xml -DskipTests install
mvn -U -s settings.local.xml -pl api test
docker compose config
git diff --check
sh -n scripts/install.sh
sh -n scripts/run.sh
sh -n scripts/init-db.sh
sh -n scripts/upgrade.sh
scripts/verify-repo.sh
```

## 本地验收记录

执行时间：2026-05-06。

- GitHub open issue 核对：通过。当前 open issue 为 #12 和 #13，本版本均已覆盖。
- OAuth 无权限提示焦点测试：通过。`OAuthActionTest` 28 tests，0 failures，0 errors。
- 第三方系统管理页焦点测试：通过。`ThirdpartJspTest`、`ThirdpartActionTest`、`ThirdpartServiceTest` 合计 29 tests，0 failures，0 errors。
- 版本切换后平台焦点回归：通过。`OAuthActionTest`、`ThirdpartJspTest`、`ThirdpartActionTest`、`ThirdpartServiceTest` 合计 57 tests，0 failures，0 errors。
- 全仓编译：通过。`mvn -s settings.local.xml -DskipTests compile`，BUILD SUCCESS。
- API 单测：通过。先执行 `mvn -s settings.local.xml -DskipTests install` 安装当前 `1.6.2` reactor 产物，再执行 `mvn -U -s settings.local.xml -pl api test`，39 tests，0 failures，0 errors。
- Compose 配置检查：通过。`docker compose config` 可生成配置。
- 脚本语法检查：通过。`install.sh`、`run.sh`、`init-db.sh`、`upgrade.sh` 均通过 `sh -n`。
- 安装脚本本地 raw 验证：通过。使用 `file://` raw 路径和 `BPMT_SKIP_UP=1` 验证默认完整库安装会生成 `db/init/bpmt.sql`，并带出 `upgrade.sh` 与 `.bpmt-lite/version`；`min` 参数另行验证可生成 `db/init/bpmt-min.sql`。
- 升级脚本 dry run：通过。使用 fake `docker` 验证 `upgrade.sh v1.6.2` 会下载 `docker-compose-v1.6.2.yml`，写入 `.env` 的 `BPMT_IMAGE_TAG=latest` 与 `BPMT_API_IMAGE_TAG=latest`，并只调用 Web/API 镜像 pull 和 `docker compose --env-file .env up -d --no-deps bpmt-web bpmt-api`。
- 仓库检查：通过。`scripts/verify-repo.sh` 输出 multi-arch 脚本检查和 repository hygiene 检查均 OK。
- 空白检查：通过。`git diff --check` 无输出。
- 本地 Web 镜像构建：通过。Docker 启动后显式切换 Java 8 执行 `scripts/build-image.sh`，Maven WAR、Docker build 和镜像内容校验均通过，生成 `ghcr.io/wodenwang/bpmt-lite:1.6.2`。
- 本地 API 镜像构建：通过。显式切换 Java 8 执行 `scripts/build-api-image.sh`，Maven WAR、Docker build 和镜像内容校验均通过，生成 `ghcr.io/wodenwang/bpmt-lite-api:1.6.2`。
- GHCR Web multi-arch 发布：通过。`ghcr.io/wodenwang/bpmt-lite:1.6.2` digest 为 `sha256:5889392bb1371d4e3323247967222cd642ffaaf8ec013ffb68354f3543313dcd`，包含 `linux/amd64` 与 `linux/arm64`。
- GHCR API multi-arch 发布：通过。`ghcr.io/wodenwang/bpmt-lite-api:1.6.2` digest 为 `sha256:f2990e708d7349b2395863c7315ec11991a0fdd5b5c6facd56350d4def15fc91`，包含 `linux/amd64` 与 `linux/arm64`。

## 发布边界

- 不升级 Java、Tomcat、MariaDB 或 nginx。
- 不改动默认 `docker-compose.yml` 中第三方容器版本策略。
- 不向业务数据库写入安装或升级状态。
- 不新增 OAuth、API 或 H5 业务能力。
