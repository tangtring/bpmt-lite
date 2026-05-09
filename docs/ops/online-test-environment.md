# 线上测试环境交接记录

本文记录 `bpmt-lite` 线上测试环境的非敏感连接信息、部署结构和已验证排障线索。

敏感信息不要写入本文。SSH 密码、数据库密码、第三方 `clientSecret`、微信 `access token` 等只允许保存在本机忽略文件 `docs/ops/local-secrets.md`，或由用户在当次会话中提供。

## 环境入口

- 服务器 IP：`120.24.236.92`
- SSH 用户：`bpmt`
- BPMT 部署目录：`/home/bpmt/bpmt-lite`
- OAuth demo 部署目录：`/home/bpmt/bpmt-oauth-demo`
- BPMT 运行方式：`docker compose`
- 当前已知 BPMT 版本：`1.6.1`
- 第三方 demo：留言板应用，通过 `bpmt-lite` OAuth 鉴权登录。

## BPMT Compose 约定

在 `/home/bpmt/bpmt-lite` 下检查：

```bash
docker compose ps
docker compose config
docker logs bpmt-web
docker logs bpmt-api
```

当前线上测试环境服务名沿用仓库默认值：

- `bpmt-nginx`
- `bpmt-web`
- `bpmt-api`
- `bpmt-mariadb`

数据库默认值仍按 compose 约定处理：

- `DB_NAME` 未覆盖时为 `bpmt`
- `DB_PASSWORD` 未覆盖时为 `123456`

排障时优先从 `.env` 读取实际覆盖值，不要假设一定是默认值。

## OAuth Demo 配置线索

线上测试环境中已见第三方系统：

- `CM_THIRDPART.CLIENT_ID=oauth-demo`
- `CM_THIRDPART.THIRDPART_KEY=oauth-demo`
- `WECHAT_LOGIN_ENABLED=1`
- `WECHAT_TYPE=agent`
- `WECHAT_KEY=3Y3_PoSxX9X`
- `REDIRECT_URIS=https://oauth-demo.riversoft.com.cn/oauth/callback`

这些值是排障线索，不代表生产配置模板。后续如用户调整线上数据，应以数据库实时查询结果为准。

## 2026-05-05 微信 OAuth 登录问题结论

现象：

- `/oauth/OAuthAction/authorize.shtml` 进入微信企业号登录。
- 企业微信返回 `UserId=woden` 且 `errcode=0`。
- BPMT 随后记录 `wechat_login_failed`，页面显示 OAuth 微信登录失败。

已确认：

- 微信侧授权成功，不是微信 code 或企业号接口失败。
- `US_USER.USER_ID=woden` 存在，`ACTIVE_FLAG=1`。
- `woden` 有 `US_USER_GROUP_ROLE` 组织角色关系。
- `oauth-demo` 第三方系统启用了微信登录，且绑定 `WECHAT_TYPE=agent`、`WECHAT_KEY=3Y3_PoSxX9X`。

根因：

- 容器内 `safe.properties` 为 `safe.role=DEV_SYS`、`safe.admin=admin`。
- `DEV_SYS` 启动时 `InitServlet` 会调用 `Platform.pause()`，平台进入暂停状态。
- `SessionManager.doUserLogin()` 对非 `safe.admin` 用户会拒绝登录并抛出“系统维护中,暂停用户登陆.”。
- `admin` 能走通是因为在 `safe.admin` 中；`woden` 不是 `safe.admin`，所以企业微信拿到 `UserId=woden` 后，本地 BPMT 登录态建立失败。
- 当前 `OAuthWechatLoginService` 将该运行时异常包装为泛化的 `wechat_login_failed / 微信登录失败`。

推荐修复：

- 线上测试环境不要用 `safe.role=DEV_SYS` 跑普通用户 OAuth。
- 将 `safe.role` 覆盖为 `LIGHT_WEIGHT` 或 `PRO_SYS` 后重启 `bpmt-web`。
- 推荐通过 `config/overrides/safe.properties` 做本地部署覆盖，不改镜像内文件。

## 常用只读 SQL

进入服务器后可执行：

```bash
cd /home/bpmt/bpmt-lite
DB_NAME=$(grep -E '^DB_NAME=' .env 2>/dev/null | tail -1 | cut -d= -f2-)
DB_PASSWORD=$(grep -E '^DB_PASSWORD=' .env 2>/dev/null | tail -1 | cut -d= -f2-)
DB_NAME=${DB_NAME:-bpmt}
DB_PASSWORD=${DB_PASSWORD:-123456}

docker exec -i bpmt-mariadb mariadb -uroot -p"$DB_PASSWORD" -D "$DB_NAME" -N -B <<'SQL'
SELECT USER_ID, BUSI_NAME, ACTIVE_FLAG, IFNULL(ALLOW_IP,''), IFNULL(WXID,''), WX_ENABLE, IFNULL(WX_STATUS,'')
FROM US_USER
WHERE USER_ID='woden';

SELECT USER_ID, GROUP_KEY, ROLE_KEY, DEFAULT_FLAG, SORT
FROM US_USER_GROUP_ROLE
WHERE USER_ID='woden';

SELECT THIRDPART_KEY, CLIENT_ID, ACTIVE_FLAG, PRI_KEY, WECHAT_LOGIN_ENABLED,
       IFNULL(WECHAT_TYPE,''), IFNULL(WECHAT_KEY,''), IFNULL(WECHAT_SCOPE,''), IFNULL(REDIRECT_URIS,'')
FROM CM_THIRDPART
WHERE CLIENT_ID='oauth-demo' OR THIRDPART_KEY='oauth-demo';
SQL
```
