# v1.5.0 发布记录

`v1.5.0` 是外部系统 OAuth 登录版本。版本目标是在不改动独立 `bpmt-api` 业务接口的前提下，让 BPMT Web 作为 OAuth2 Authorization Code 服务端，复用现有用户、登录页和权限体系，为第三方 Web 系统提供统一登录入口。

## 版本范围

- 新增外部系统主数据 `CM_THIRDPART`。
- 新增 OAuth 授权码表 `CM_THIRDPART_AUTH_CODE` 和 access token 表 `CM_THIRDPART_ACCESS_TOKEN`。
- 新增 OAuth 端点：`/oauth/authorize`、`/oauth/token`、`/oauth/userinfo`。
- 新增后台菜单 `系统开发 -> 第三方系统`。
- 权限组管理新增 `第三方系统权限` 页签，用于绑定外部系统访问权限。
- 用户菜单支持 `第三方网页` 打开类型，并在 BPMT 主工作区内嵌第三方页面。
- OAuth 主流程在 `bpmt-web/platform` 内闭环，不改 `bpmt-api`。
- 不实现 OIDC，不提供 `refresh_token`，不提供 `userid + thirdpartKey` 独立权限校验 API。

## 安全与运行态

- `code` 和 `access_token` 只保存 hash，明文只返回给调用方一次。
- `clientSecret` 只保存 hash，新建时只展示一次，编辑时只允许重置。
- `/oauth/token` 和 `/oauth/userinfo` 设置 `Cache-Control: no-store` 与 `Pragma: no-cache`。
- `/oauth/userinfo` 不接受已删除用户的有效 token。
- OAuth 主流程按 `INFO` 记录关键状态，日志禁止记录明文 `code`、`access_token`、`client_secret`、`password`。

## 验收结论

- `docker compose config` 通过，compose 服务名与固定容器名统一为 `bpmt-nginx`、`bpmt-web`、`bpmt-api`、`bpmt-mariadb`。
- Java 8 全仓编译通过：`mvn -s settings.local.xml -DskipTests compile`。
- API 单测通过：`mvn -s settings.local.xml -pl api test`，共 39 个测试。
- OAuth/第三方系统目标测试通过：共 55 个测试。
- `scripts/build-image.sh` 通过，生成并验证 Web 镜像 `ghcr.io/wodenwang/bpmt-lite:1.5.0`。
- `scripts/build-api-image.sh` 通过，生成并验证 API 镜像 `ghcr.io/wodenwang/bpmt-lite-api:1.5.0`。
- 临时发布环境使用最小库 `bpmt_min` 验证通过：`/`、`/ueditor/`、`/api/openapi.json`、`/api/docs/` 均返回 200。
- `scripts/smoke-api.sh` 在临时发布环境通过。
- 最小库表数量为 176，其中包含 3 张 OAuth 登录表。

详细 OAuth 验收清单见 [docs/v1.5.0/oauth-login-acceptance.md](v1.5.0/oauth-login-acceptance.md)。

## 发布信息

- Git tag：`v1.5.0`
- Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.5.0`，digest `sha256:fa7d1c3c68ca045e4c0232c54c2ad1305489765ed7b33e62f72c57b745c4d186`
- API 镜像：`ghcr.io/wodenwang/bpmt-lite-api:1.5.0`，digest `sha256:bf40b065c59743f129179ed0483f22e39420e56350fc3e11b6fafcd70c716f87`
- 同步镜像：`ghcr.io/wodenwang/bpmt-lite:latest`、`ghcr.io/wodenwang/bpmt-lite-api:latest`
- `latest` 已同步到 `1.5.0` digest。
