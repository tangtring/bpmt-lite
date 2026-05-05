# v1.6.1 发布记录草案

## 版本定位

`v1.6.1` 是基于 `v1.6.0` 的补丁版本，目标是在不改变外部系统标准 OAuth 接入方式的前提下，增强微信生态下第三方 OAuth 登录态传导。

外部系统仍按标准 `/oauth/authorize` 发起登录。微信内、无 BPMT 登录态、且第三方启用微信登录绑定时，BPMT 先走企业号或服务号微信 OAuth；微信登录成功后回到原 authorize 请求，继续签发标准 OAuth code，第三方仍通过原 `redirect_uri?code=...&state=...` 接收结果。

## 主要变化

- `CM_THIRDPART` 增加微信登录绑定配置，已有数据库升级需执行 `database/v1.6.1-wechat-oauth-thirdpart.sql`。
- 第三方微信登录默认关闭；未配置第三方行为与 `v1.6.0` 一致。
- 微信登录配置由第三方系统记录明确指定，不按 UA 自动猜测企业号或服务号。
- 本机验收通过 fake provider 完成微信 code 回来后的 BPMT session 写入和第三方 OAuth code 回调链路。
- 真实企业号或服务号微信 OAuth 只作为部署后人工验收项。

## 验收命令

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -pl platform -am -DfailIfNoTests=false -Dtest=OAuthActionTest,OAuthWechatLoginServiceTest,ThirdpartServiceTest,ThirdpartActionTest,ThirdpartJspTest,OAuthHbmMappingTest,OAuthDatabaseInitSqlTest test
mvn -s settings.local.xml -DskipTests compile
mvn -s settings.local.xml -DskipTests install
mvn -U -s settings.local.xml -pl api test
scripts/build-image.sh
scripts/build-api-image.sh
scripts/verify-repo.sh
git diff --check
git status --short
```

## Task 8 版本收口验收

执行时间：2026-05-05。

- 版本引用检查：通过。Maven 项目版本、默认 Web/API 镜像 tag、`install.sh`/`run.sh` 默认 `BPMT_REF`、`init-db.sh` 默认 raw tag、README 当前安装命令和 OpenAPI 版本字段已切到 `1.6.1` / `v1.6.1`。剩余 `1.6.0` / `v1.6.0` 均为基线或历史说明。
- 平台窄测：通过。原 `mvn -s settings.local.xml -pl platform ... test` 在 release 版本刚切换后因未带 `-am` 无法解析本地未安装的 `1.6.1` 内部依赖；改用 `mvn -s settings.local.xml -pl platform -am -DfailIfNoTests=false ... test` 后通过，75 tests，0 failures，0 errors。
- 全仓编译：通过。`mvn -s settings.local.xml -DskipTests compile`，BUILD SUCCESS。
- API 测试：通过。原 `mvn -s settings.local.xml -pl api test` 在内部 `1.6.1` 依赖未安装时解析失败；先执行 `mvn -s settings.local.xml -DskipTests install` 安装当前 reactor 产物到本地 Maven 仓库，再执行 `mvn -U -s settings.local.xml -pl api test` 通过，39 tests，0 failures，0 errors。
- Web 镜像构建：通过。`scripts/build-image.sh` 生成并验证 `ghcr.io/wodenwang/bpmt-lite:1.6.1`。
- API 镜像构建：通过。`scripts/build-api-image.sh` 生成并验证 `ghcr.io/wodenwang/bpmt-lite-api:1.6.1`。
- 仓库检查：通过。`scripts/verify-repo.sh` 输出 `OK: multi-arch image build script checks passed` 和 `OK: repository hygiene checks passed`。
- 空白检查：通过。`git diff --check` 无输出。
- 工作区检查：通过。`git status --short` 在提交前仅显示本次版本收口文件，提交后工作区干净。

## 发布边界

- 不新增第三方通知 API。
- 不实现 OIDC、`refresh_token` 或单点登出。
- 不改变 `bpmt-api` 对外业务接口。
- 不改变外部系统标准 OAuth 接入端点和 code 回调协议。
- 不默认启用 fake provider；fake provider 只能通过显式环境变量、系统属性或 smoke 临时启用。
