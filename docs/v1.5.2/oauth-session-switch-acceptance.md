# v1.5.2 OAuth 登录态切换验收记录

## 1. 验收目标

- 第三方系统未登录时，继续由第三方系统跳转 BPMT `/oauth/authorize`。
- 浏览器已有 BPMT 登录态时，BPMT 复用当前用户，不强制显示登录页。
- 当前 BPMT 用户无目标第三方权限时，显示 BPMT 内部提示页。
- 用户可选择退出当前账号并重新登录。
- 用户可选择取消并返回第三方 `access_denied`。
- `v1.5.1` issue #10 工作流待办“查看/处理”跳转修复继续保留，不出现 `_ORD_ID=null`。

## 2. 验收环境

- 验收时间：2026-05-04
- Git branch：`codex/v1.5.2-oauth-session-switch`
- 验收前提交：`73c6c65 docs(v1.5.2): update maintenance baseline`
- 临时运行目录：`/tmp/bpmt-v152-Zj3DMc`
- Compose project：`bpmt-v152`
- 临时容器名：`bpmt-v152-nginx`、`bpmt-v152-web`、`bpmt-v152-api`、`bpmt-v152-mariadb`
- 浏览器 / API base URL：`http://127.0.0.1:18080`
- 数据库端口：`127.0.0.1:13306`
- 数据库：完整库 `bpmt`，由 `database/bpmt.sql.gz` 解压到临时目录 `db/init/bpmt.sql` 初始化
- 完整库表数：`380`
- Web image：`ghcr.io/wodenwang/bpmt-lite:1.5.2`
- API image：`ghcr.io/wodenwang/bpmt-lite-api:1.5.2`
- 本地镜像 digest：
  - `ghcr.io/wodenwang/bpmt-lite:1.5.2`：`sha256:6141d758529388af1231c1911455b4da662e59f64df71c268c9439af9372bdf7`
  - `ghcr.io/wodenwang/bpmt-lite-api:1.5.2`：`sha256:70b415ddf2baad12c7abf771c3ddeb6bff82303e7cbebb7e02fdabdddcec1a24`

本机已有默认 `bpmt-*` 容器，其中 `bpmt-mariadb` 正在运行。为避免影响既有运行态，本次只在临时目录副本中把 `container_name` 改为 `bpmt-v152-*`，未修改仓库 `docker-compose.yml`。

临时 nginx 副本沿用仓库配置启动后，`/oauth/authorize` 会因 `proxy_set_header Host $host;` 丢失端口并 302 到 `http://127.0.0.1/login.jsp`。本次仅在临时目录 `/tmp/bpmt-v152-Zj3DMc/docker/nginx/nginx.conf` 中改为 `proxy_set_header Host $http_host;` 并重建 `bpmt-v152-nginx`，仓库文件未修改。

## 3. 代码与构建门禁

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| 分支确认 | `git status --short --branch` | `## codex/v1.5.2-oauth-session-switch` |
| 最近提交 | `git log --oneline -8` | 最新为 `73c6c65 docs(v1.5.2): update maintenance baseline` |
| OAuth 窄测试 | `mvn -s settings.local.xml -pl platform -am -Dtest=OAuthActionTest,OAuthLoginReturnTest -DfailIfNoTests=false test` | `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| OAuth 全测试 | `mvn -s settings.local.xml -pl platform -am -Dtest=OAuthActionTest,OAuthServiceTest,OAuthSecurityTest,OAuthHbmMappingTest,OAuthLoginReturnTest,OAuthDatabaseInitSqlTest -DfailIfNoTests=false test` | `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| 全仓编译 | `mvn -s settings.local.xml -DskipTests compile` | `BUILD SUCCESS` |
| Compose 配置 | `docker compose config` | 成功解析，默认镜像 tag 为 `1.5.2` |
| Web 镜像 | `scripts/build-image.sh` | 首次未显式设置 Java 8 时被脚本拒绝：`当前 Java 版本不是 Java 8：java version "25.0.1"`；设置 Java 8 后通过，`Docker image verified: ghcr.io/wodenwang/bpmt-lite:1.5.2` |
| API 镜像 | `scripts/build-api-image.sh` | 设置 Java 8 后通过，`Docker image verified: ghcr.io/wodenwang/bpmt-lite-api:1.5.2` |

构建使用的 Java 环境：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

## 4. 临时运行时启动

```bash
tmpdir="$(mktemp -d /tmp/bpmt-v152-XXXXXX)"
cp docker-compose.yml "$tmpdir/docker-compose.yml"
mkdir -p "$tmpdir/docker/nginx" "$tmpdir/config/overrides" "$tmpdir/db/init"
cp docker/nginx/nginx.conf "$tmpdir/docker/nginx/nginx.conf"
gzip -dc database/bpmt.sql.gz > "$tmpdir/db/init/bpmt.sql"
perl -0pi -e 's/container_name: bpmt-nginx/container_name: bpmt-v152-nginx/g; s/container_name: bpmt-web/container_name: bpmt-v152-web/g; s/container_name: bpmt-api/container_name: bpmt-v152-api/g; s/container_name: bpmt-mariadb/container_name: bpmt-v152-mariadb/g' "$tmpdir/docker-compose.yml"
cd "$tmpdir"
BPMT_HTTP_PORT=18080 BPMT_DB_PORT=13306 BPMT_IMAGE_TAG=1.5.2 BPMT_API_IMAGE_TAG=1.5.2 docker compose -p bpmt-v152 up -d
```

`docker compose -p bpmt-v152 ps` 显示四个临时容器均已启动，`bpmt-v152-mariadb` 为 `healthy`。Web/API Hazelcast 日志均出现 `Members [2]`。

## 5. HTTP / API 基线

| 路径 / 命令 | 结果 |
| --- | --- |
| `GET /` | `200 http://127.0.0.1:18080/` |
| `GET /ueditor/` | `200 http://127.0.0.1:18080/ueditor/` |
| `GET /api/docs/` | `200 http://127.0.0.1:18080/api/docs/` |
| `GET /api/openapi.json` | `200 http://127.0.0.1:18080/api/openapi.json` |
| `GET /oauth/authorize` | `200 http://127.0.0.1:18080/oauth/authorize` |
| `PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin BPMT_API_BASE_URL=http://127.0.0.1:18080/api scripts/smoke-api.sh` | `API smoke passed: http://127.0.0.1:18080/api` |

补充：直接运行 `BPMT_API_BASE_URL=http://127.0.0.1:18080/api scripts/smoke-api.sh` 时，本机 shell 环境中 `curl` / `grep` 未被脚本找到；显式设置系统 `PATH` 后通过。

## 6. OAuth 浏览器验收

初始化库没有现成 OAuth client 和非管理员权限组合。本次只在临时数据库中新增验收客户端和独立权限点，不提交数据库变更：

```sql
insert into cm_pri
  (PRI_KEY,CATELOG_TYPE,CATELOG_KEY,BUSI_NAME,DESCRIPTION,TYPE,CHECK_TYPE,CHECK_SCRIPT)
values
  ('oauth_v152_demo',1,'oauth_v152_demo','v1.5.2 OAuth 验收权限','临时运行态 OAuth session switch 验收权限',1,2,'${true}')
on duplicate key update BUSI_NAME=values(BUSI_NAME);

insert into cm_thirdpart
  (THIRDPART_KEY,THIRDPART_NAME,CLIENT_ID,CLIENT_SECRET_HASH,REDIRECT_URIS,HOME_URL,PRI_KEY,ACTIVE_FLAG,DESCRIPTION,CREATE_TIME,UPDATE_TIME)
values
  ('oauth_v152_demo','v1.5.2 OAuth 验收客户端','client-v152-smoke','dummy-secret-hash','http://127.0.0.1:18080/oauth-smoke/callback',NULL,'oauth_v152_demo',1,'临时运行态 OAuth session switch 验收客户端',now(),now())
on duplicate key update REDIRECT_URIS=values(REDIRECT_URIS), PRI_KEY=values(PRI_KEY), ACTIVE_FLAG=1, UPDATE_TIME=now();
```

测试 URL：

```text
http://127.0.0.1:18080/oauth/authorize?response_type=code&client_id=client-v152-smoke&redirect_uri=http%3A%2F%2F127.0.0.1%3A18080%2Foauth-smoke%2Fcallback&state=s-1
```

测试账号：

- `admin/admin`：系统管理员，作为有权限账号。
- `zhangzongcai/123`：普通业务账号，未绑定 `oauth_v152_demo` 权限点，作为无权限账号。

Playwright 浏览器验收结果：

| 场景 | 结果 |
| --- | --- |
| 无 BPMT session 访问 authorize | 跳转到 `http://127.0.0.1:18080/login.jsp`，页面显示 `请先登录.` |
| 已有 BPMT session + `admin` | 回跳 `http://127.0.0.1:18080/oauth-smoke/callback?code=...&state=s-1`，包含 `code` 和 `state=s-1` |
| 已有 BPMT session + `zhangzongcai` 无权限 | 停留 authorize URL，页面显示 `当前账号无权访问外部系统`，正文包含当前账号 `zhangzongcai` 和客户端 `v1.5.2 OAuth 验收客户端` |
| 无权限页点击“取消并返回第三方系统” | 回跳 `http://127.0.0.1:18080/oauth-smoke/callback?error=access_denied&...&state=s-1` |
| 无权限页点击“退出当前账号并重新登录” | 跳转到 `http://127.0.0.1:18080/login.jsp`，页面显示 `请先登录.` |
| 切换账号后以 `admin/admin` 登录 | 继续原 authorize，回跳 `http://127.0.0.1:18080/oauth-smoke/callback?code=...&state=s-1` |

Playwright 原始结果摘要：

```json
[
  {"case":"no_session_login_page","url":"http://127.0.0.1:18080/login.jsp","body":"请先登录.\\n\\nCopyright (c) 2026 wodenwang and borballzhai"},
  {"case":"allowed_admin_code","hasCode":true,"hasState":true},
  {"case":"denied_prompt","contains":true,"user":true},
  {"case":"cancel_redirect","hasAccessDenied":true,"hasState":true},
  {"case":"switch_to_login","url":"http://127.0.0.1:18080/login.jsp","isLogin":true},
  {"case":"switch_admin_code","hasCode":true,"hasState":true}
]
```

## 7. issue #10 回归

复用完整库临时运行时，按 `docs/v1.5.1/issue-10-acceptance.md` 的代表账号执行浏览器实点：

- 登录账号：`zhangzongcai/123`
- 入口：`/flow/CommonFlowAction/taskList.shtml`
- 待办单号：`FNBW2604001`
- Task ID：`b26481fc-42d3-11f1-b6d7-de29797a6eb7`

点击“查看”：

- `POST /flow/CommonFlowAction/detail.shtml`
- `GET /1iI5xylQL9X.view?...&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null`
- 后续 `sub.shtml`、`pictureMain.shtml`、`history.shtml`、`nodeDetail.shtml`、`picture.shtml` 均返回业务视图相关请求。
- 网络请求不包含 `_ORD_ID=null`。
- 页面正文包含 `FNBW2604001` / `员工借款`。
- 浏览器 console error 为空，page error 为空。

点击“处理”：

- `POST /flow/CommonFlowAction/form.shtml`
- `GET /1iI5xylQL9X.view?...&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null`
- 后续 `sub.shtml`、`pictureMain.shtml`、`history.shtml`、`nodeDetail.shtml`、`picture.shtml` 均返回业务视图相关请求。
- 网络请求不包含 `_ORD_ID=null`。
- 页面正文包含 `FNBW2604001` / `员工借款`。
- 浏览器 console error 为空，page error 为空。

## 8. 结论

本地代码门禁、镜像构建、完整库临时 runtime、HTTP/API baseline、OAuth session switch 浏览器验收、issue #10 浏览器回归均已通过。镜像推送、tag、GitHub Release 是否完成以发布命令实际结果为准，不在本节提前声明。
