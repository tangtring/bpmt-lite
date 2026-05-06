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
git diff --check
sh -n scripts/install.sh
sh -n scripts/run.sh
sh -n scripts/init-db.sh
sh -n scripts/upgrade.sh
```

## 发布边界

- 不升级 Java、Tomcat、MariaDB 或 nginx。
- 不改动默认 `docker-compose.yml` 中第三方容器版本策略。
- 不向业务数据库写入安装或升级状态。
- 不新增 OAuth、API 或 H5 业务能力。
