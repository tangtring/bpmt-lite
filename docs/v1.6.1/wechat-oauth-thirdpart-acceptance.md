# v1.6.1 微信 OAuth 第三方登录验收

本文档用于把“本机无法真实微信登录”的限制落到可执行验收：本机使用显式启用的 fake provider 做自动化 smoke，真实微信环境继续做人工验收。

## 本机 fake provider 验收

fake provider 只用于本机 smoke，不是默认行为。只有满足以下任一条件时才启用：

- 环境变量 `BPMT_OAUTH_WECHAT_FAKE_PROVIDER=true` 或 `BPMT_OAUTH_WECHAT_FAKE_PROVIDER=1`
- 系统属性 `bpmt.oauth.wechat.fake.provider=true` 或 `bpmt.oauth.wechat.fake.provider=1`

默认 fake code 是 `fake-admin`，可通过 `BPMT_OAUTH_WECHAT_FAKE_CODE` 或 `bpmt.oauth.wechat.fake.code` 覆盖。受控 code 映射如下：

- `fake-admin`：登录为 `admin`
- `fake-user-no-pri`：登录为 `oauth_no_pri`
- `fake-invalid`：返回微信登录失败

自动化 smoke：

```bash
bash scripts/smoke-oauth-wechat.sh
```

执行前确认：

- `docker compose ps` 中 `bpmt-mariadb` 可用。
- 当前运行数据库包含 `CM_THIRDPART`、`CM_THIRDPART_AUTH_CODE`、`CM_THIRDPART_ACCESS_TOKEN`。
- 接受脚本通过临时 compose override recreate `bpmt-web` 和 `bpmt-nginx`，注入 `BPMT_OAUTH_WECHAT_FAKE_PROVIDER=true` 和 `BPMT_OAUTH_WECHAT_FAKE_CODE=fake-admin`。
- 默认入口仍是 nginx 的 `http://127.0.0.1/`，脚本会等待入口可用后再发起 OAuth 请求。
- 默认 `CLIENT_ID=wechat-smoke-client`。如果覆盖 `CLIENT_ID`，必须以 `wechat-smoke-` 开头，避免覆盖真实第三方配置。

脚本验收点：

- 非微信 UA 访问 `/oauth/authorize` 时仍落到 BPMT 普通登录页。
- 微信 UA 首次访问 `/oauth/authorize` 时重定向回同一 authorize URL，并追加受控 fake code。
- 携带 cookie 访问 fake callback 后，BPMT 建立登录态并签发 OAuth 授权码。
- 最终跳转到第三方 `redirect_uri`，并带回 `code` 与原始 `state`。
- 脚本输出不打印完整 OAuth code。
- 退出时脚本删除临时 compose override，并执行正常 `docker compose up -d --force-recreate bpmt-web bpmt-nginx`，恢复真实 provider。
- 退出时脚本会清理 `CM_THIRDPART.THIRDPART_KEY='wechat-smoke'` 的 smoke 记录，并仅按 `THIRDPART_KEY='wechat-smoke'` 清理对应 OAuth code/token 运行数据。

异常处理要求：

- 即使 smoke 中途失败，也必须执行退出清理和容器恢复。
- 恢复后应尽量确认 `bpmt-web` 容器环境中不再包含 `BPMT_OAUTH_WECHAT_FAKE_PROVIDER=true`。
- 如果当前库缺少 v1.6.1 微信登录字段，脚本只按 `database/v1.6.1-wechat-oauth-thirdpart.sql` 的语义补齐字段，不做其他 schema 修改。

## 真实微信人工验收

真实环境不启用 fake provider，保持默认 `RealWechatOAuthProvider`。

人工 checklist：

- `CM_THIRDPART.WECHAT_LOGIN_ENABLED=1`，`WECHAT_TYPE`、`WECHAT_KEY`、`WECHAT_SCOPE` 与实际企业微信或服务号配置一致。
- 第三方系统 `CLIENT_ID`、`REDIRECT_URIS`、`PRI_KEY`、`ACTIVE_FLAG` 配置正确。
- 非微信浏览器访问第三方 OAuth 入口时，仍进入 BPMT 普通登录流程。
- 微信内访问第三方 OAuth 入口时，进入真实微信授权或静默登录流程。
- 微信授权成功后，BPMT 用户登录态建立，并按 `PRI_KEY` 做第三方系统权限校验。
- 有权限用户最终返回第三方回调地址，并携带 OAuth `code` 与原始 `state`。
- 无权限用户进入 BPMT 授权拒绝提示，可切换账号或取消。
- `/oauth/token` 可用授权码换取 access token，`/oauth/userinfo` 可用 token 读取用户信息。

## 日志安全

日志不得包含以下明文：

- OAuth code
- 微信 code
- access token
- client secret
- 密码

允许记录 `clientId`、`thirdpartKey`、`wechatType`、`wechatKey`、`userId`、`requestId`、结果状态和稳定 reason。排查问题时优先使用 requestId、reason 和服务端状态，不要把敏感凭据写入日志或验收记录。
