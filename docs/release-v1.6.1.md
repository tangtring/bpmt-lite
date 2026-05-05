# v1.6.1 发布记录

## 版本定位

`v1.6.1` 是基于 `v1.6.0` 的补丁版本，目标是在不改变外部系统标准 OAuth 接入方式的前提下，增强微信生态下第三方 OAuth 登录态传导。

外部系统仍按标准 `/oauth/authorize` 发起登录。微信内、无 BPMT 登录态、且第三方启用微信登录绑定时，BPMT 先走企业号或服务号微信 OAuth；微信登录成功后回到原 authorize 请求，继续签发标准 OAuth code，第三方仍通过原 `redirect_uri?code=...&state=...` 接收结果。

## 主要变化

- `CM_THIRDPART` 增加微信登录绑定配置，已有数据库升级需执行 `database/v1.6.1-wechat-oauth-thirdpart.sql`。
- 第三方微信登录默认关闭；未配置第三方行为与 `v1.6.0` 一致。
- 微信登录配置由第三方系统记录明确指定，不按 UA 自动猜测企业号或服务号。
- 本机验收通过 fake provider 完成微信 code 回来后的 BPMT session 写入和第三方 OAuth code 回调链路。
- 最终评审 P1 已修复：企业号微信 OAuth 授权和 code 登录均按第三方系统绑定的 `WECHAT_KEY` 加载 `WxAgent`，配置缺失或不完整时返回 `wechat_config_invalid`，不再静默回退默认企业号。
- fake smoke 复测补齐了微信登录成功后刷新当前线程 `SessionContext` 的场景，避免同一个 authorize 请求内签发第三方 OAuth code 时仍读到旧登录态。
- 最终评审 P1 复查已修复：服务号真实登录分支在 `WxActionAspect.mpCodeLogin()` 写入会话后直接从 `HttpSession` 读取 BPMT 用户，避免 provider 内部读取旧 `SessionContext` 导致误判登录失败。
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

## Task 9 微信 fake smoke 验收

执行时间：2026-05-05。

- 微信 fake 冒烟：通过。使用当前本机最小库执行 `DB_NAME=bpmt_min scripts/smoke-oauth-wechat.sh`，脚本完成非微信 UA 回落 BPMT 登录页、微信 UA 重定向 fake callback、fake callback 建立 BPMT 登录态并回跳第三方 callback 三段验证。
- 运行态恢复：通过。脚本退出后 `bpmt-web` 环境变量中无 `BPMT_OAUTH_WECHAT_FAKE_PROVIDER` / `BPMT_OAUTH_WECHAT_FAKE_CODE`，仅保留本轮 compose 使用的 `DB_NAME=bpmt_min`。
- 数据清理：通过。`CM_THIRDPART`、`CM_THIRDPART_AUTH_CODE`、`CM_THIRDPART_ACCESS_TOKEN` 中 `THIRDPART_KEY='wechat-smoke'` 的记录均为 0。
- 本机覆盖隔离：通过。脚本临时设置 `SAFE_ROLE=LIGHT_WEIGHT` 并挂载临时 `safe.properties`，避免本机 `config/overrides/safe.properties` 覆盖 `safe.admin` 影响 fake 登录验收；退出后临时文件删除，不修改本机配置。
- 修复验证：通过。`OAuthWechatLoginService` 在微信 provider 登录成功后刷新当前线程 `SessionContext`，`OAuthWechatLoginServiceTest` 新增覆盖并通过，确保同一请求内 `OAuthService.currentUserCanAccess()` 可读取刚建立的 BPMT 登录态。
- 服务号分支复查：通过。`RealWechatOAuthProvider` 的服务号路径改为从 `HttpSession` 读取 `USER`，新增 `realProviderReadsMpLoggedInUserFromHttpSession` 覆盖，避免真实服务号 code 登录在 provider 内部读取旧 `SessionContext`。

## Task 10 multi-arch 镜像发布验收

执行时间：2026-05-05。

- 正式镜像发布：通过。执行 `scripts/build-multiarch-images.sh`，重新构建并推送 Web/API 双镜像。
- Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.6.1`，manifest digest `sha256:e629f1889f198d0c19b9b811472442d6ba5557216cc62a5145b1a42c6875c2d8`，包含 `linux/amd64` 和 `linux/arm64`。
- API 镜像：`ghcr.io/wodenwang/bpmt-lite-api:1.6.1`，manifest digest `sha256:beea3c5c36c41f5c38542c37adbbc3aed29fecc7680253fe4ac65ec379d63c25`，包含 `linux/amd64` 和 `linux/arm64`。
- `latest` 同步：通过。`ghcr.io/wodenwang/bpmt-lite:latest` 和 `ghcr.io/wodenwang/bpmt-lite-api:latest` 已随脚本同步推送。
- manifest inspect：通过。脚本末尾已执行 `docker buildx imagetools inspect` 并列出两个正式镜像的 `linux/amd64`、`linux/arm64` manifest。

## Task 11 GitHub 发布和公开安装验收

执行时间：2026-05-05。

- GitHub 推送：通过。已推送 `main`，并创建远端 tag `v1.6.1`；发布 tag 指向 `7d86234`。
- GitHub Release：通过。Release 地址为 `https://github.com/wodenwang/bpmt-lite/releases/tag/v1.6.1`。
- 公开一行安装：通过。使用 `https://github.com/wodenwang/bpmt-lite/raw/refs/tags/v1.6.1/scripts/install.sh` 在临时目录安装最小库，端口覆盖为 `BPMT_HTTP_PORT=18082`、`BPMT_DB_PORT=13308`。
- 公开安装 smoke：通过。`/`、`/ueditor/`、`/api/docs/`、`/api/openapi.json` 均返回 200，OpenAPI 版本为 `1.6.1`。
- 最小库表数：通过。公开安装生成的 `bpmt_min` 包含 176 张表。
- 本机运行态恢复：通过。公开安装临时 compose 已停止，本仓原 `DB_NAME=bpmt_min docker compose up -d` 已恢复。

## 发布边界

- 不新增第三方通知 API。
- 不实现 OIDC、`refresh_token` 或单点登出。
- 不改变 `bpmt-api` 对外业务接口。
- 不改变外部系统标准 OAuth 接入端点和 code 回调协议。
- 不默认启用 fake provider；fake provider 只能通过显式环境变量、系统属性或 smoke 临时启用。
