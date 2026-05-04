# v1.5.3 OAuth 登录态切换与 nginx Host 验收记录

## 1. 验收目标

- 使用仓库内 `docker/nginx/nginx.conf`，不再依赖临时 nginx 配置补丁。
- `BPMT_HTTP_PORT=18080` 等非 80 端口运行时，OAuth 登录页、授权页和第三方回调地址保留实际端口。
- 第三方系统未登录时，继续由第三方系统跳转 BPMT `/oauth/authorize`。
- 浏览器已有 BPMT 登录态时，BPMT 复用当前用户，不强制显示登录页。
- 当前 BPMT 用户无目标第三方权限时，显示 BPMT 内部提示页。
- 用户可选择退出当前账号并重新登录。
- 用户可选择取消并返回第三方 `access_denied`。
- `v1.5.1` issue #10 工作流待办“查看/处理”跳转修复继续保留，不出现 `_ORD_ID=null`。

## 2. 验收环境

- 验收时间：2026-05-04
- Git branch：`codex/v1.5.2-oauth-session-switch`
- 验收提交：本次 `v1.5.3` 发布提交 `fix(v1.5.3): preserve oauth proxy host port`
- 临时运行目录：`/tmp/bpmt-v153-znJIRj`
- Compose project：`bpmt-v153`
- 浏览器 / API base URL：`http://127.0.0.1:18080`
- 数据库端口：`127.0.0.1:13306`
- 数据库：完整库 `bpmt`，由 `database/bpmt.sql.gz` 解压初始化
- 完整库表数：`380`
- Web image：`ghcr.io/wodenwang/bpmt-lite:1.5.3`
- API image：`ghcr.io/wodenwang/bpmt-lite-api:1.5.3`
- 本地镜像 digest：
  - `ghcr.io/wodenwang/bpmt-lite:1.5.3`：`sha256:3d8af5534434e5f8b1d16ceeb95e044afb7fb97e9ef5757aa350a4c7bb3ba85c`
  - `ghcr.io/wodenwang/bpmt-lite-api:1.5.3`：`sha256:a6ce76c2020e3313eea07759e83aac72bd8f2738a2f99c64d4e5ca209e8e6c98`

本机如已有默认 `bpmt-*` 容器，应使用临时目录和临时 `container_name` 避免影响既有运行态。临时目录必须直接复制仓库 `docker/nginx/nginx.conf`，不得再改成只在临时目录生效的配置。

## 3. 代码与构建门禁

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| 分支确认 | `git status --short --branch` | `## codex/v1.5.2-oauth-session-switch` |
| 最近提交 | `git log --oneline -8` | 最新为本次 `v1.5.3` 发布提交 `fix(v1.5.3): preserve oauth proxy host port` |
| OAuth 窄测试 | `mvn -s settings.local.xml -pl platform -am -Dtest=OAuthActionTest,OAuthLoginReturnTest -DfailIfNoTests=false test` | `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS` |
| 全仓编译 | `mvn -s settings.local.xml -DskipTests compile` | `BUILD SUCCESS` |
| Compose 配置 | `docker compose config` | 成功解析，默认镜像 tag 为 `1.5.3` |
| Web 镜像 | `scripts/build-image.sh` | 通过，`Docker image verified: ghcr.io/wodenwang/bpmt-lite:1.5.3` |
| API 镜像 | `scripts/build-api-image.sh` | 通过，`Docker image verified: ghcr.io/wodenwang/bpmt-lite-api:1.5.3` |

构建使用的 Java 环境：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

## 4. 临时运行时启动

```bash
tmpdir="$(mktemp -d /tmp/bpmt-v153-XXXXXX)"
cp docker-compose.yml "$tmpdir/docker-compose.yml"
mkdir -p "$tmpdir/docker/nginx" "$tmpdir/config/overrides" "$tmpdir/db/init"
cp docker/nginx/nginx.conf "$tmpdir/docker/nginx/nginx.conf"
gzip -dc database/bpmt.sql.gz > "$tmpdir/db/init/bpmt.sql"
perl -0pi -e 's/container_name: bpmt-nginx/container_name: bpmt-v153-nginx/g; s/container_name: bpmt-web/container_name: bpmt-v153-web/g; s/container_name: bpmt-api/container_name: bpmt-v153-api/g; s/container_name: bpmt-mariadb/container_name: bpmt-v153-mariadb/g' "$tmpdir/docker-compose.yml"
cd "$tmpdir"
BPMT_HTTP_PORT=18080 BPMT_DB_PORT=13306 BPMT_IMAGE_TAG=1.5.3 BPMT_API_IMAGE_TAG=1.5.3 docker compose -p bpmt-v153 up -d
```

## 5. HTTP / API / OAuth 基线

| 路径 / 命令 | 结果 |
| --- | --- |
| `GET /` | `200 http://127.0.0.1:18080/` |
| `GET /ueditor/` | `200 http://127.0.0.1:18080/ueditor/` |
| `GET /api/docs/` | `200 http://127.0.0.1:18080/api/docs/` |
| `GET /api/openapi.json` | `200 http://127.0.0.1:18080/api/openapi.json` |
| `GET /oauth/authorize` | `200 http://127.0.0.1:18080/oauth/authorize` |
| `PATH=/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin BPMT_API_BASE_URL=http://127.0.0.1:18080/api scripts/smoke-api.sh` | `API smoke passed: http://127.0.0.1:18080/api` |

## 6. OAuth 浏览器验收

测试 URL：

```text
http://127.0.0.1:18080/oauth/authorize?response_type=code&client_id=client-v153-smoke&redirect_uri=http%3A%2F%2F127.0.0.1%3A18080%2Foauth-smoke%2Fcallback&state=s-1
```

测试账号：

- `admin/admin`：系统管理员，作为有权限账号。
- `zhangzongcai/123`：普通业务账号，未绑定本次验收权限点，作为无权限账号。

| 场景 | 结果 |
| --- | --- |
| 无 BPMT session 访问 authorize | 跳转到 `http://127.0.0.1:18080/login.jsp`，页面显示 `请先登录.`，端口保留 |
| 已有 BPMT session + `admin` | 回跳 `http://127.0.0.1:18080/oauth-smoke/callback?code=...&state=s-1`，包含 `code` 和 `state=s-1`，端口保留 |
| 已有 BPMT session + `zhangzongcai` 无权限 | 停留 authorize URL，页面显示 `当前账号无权访问外部系统`，正文包含当前账号 `zhangzongcai` 和客户端 `v1.5.3 OAuth 验收客户端`，端口保留 |
| 无权限页点击“取消并返回第三方系统” | 回跳 `http://127.0.0.1:18080/oauth-smoke/callback?error=access_denied&...&state=s-1`，端口保留 |
| 无权限页点击“退出当前账号并重新登录” | 跳转到 `http://127.0.0.1:18080/login.jsp`，页面显示 `请先登录.`，端口保留 |
| 切换账号后以 `admin/admin` 登录 | 继续原 authorize，回跳 `http://127.0.0.1:18080/oauth-smoke/callback?code=...&state=s-1`，端口保留 |

## 7. issue #10 回归

复用完整库临时运行时，按 `docs/v1.5.1/issue-10-acceptance.md` 的代表账号执行浏览器实点：

- 登录账号：`zhangzongcai/123`
- 入口：`/flow/CommonFlowAction/taskList.shtml`
- 待办单号：`FNBW2604001`
- Task ID：`b26481fc-42d3-11f1-b6d7-de29797a6eb7`

点击“查看”：

- `GET /1iI5xylQL9X.view?...&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null`
- 网络请求不包含 `_ORD_ID=null`。
- 页面正文包含 `FNBW2604001` / `员工借款`。

点击“处理”：

- `GET /1iI5xylQL9X.view?...&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null`
- 网络请求不包含 `_ORD_ID=null`。
- 页面正文包含 `FNBW2604001` / `员工借款`。

## 8. 结论

本地代码门禁、镜像构建、完整库临时 runtime、HTTP/API baseline、仓库 nginx 配置下的 OAuth session switch 浏览器验收、issue #10 浏览器回归均已通过。`v1.5.3` 修复后，在 `BPMT_HTTP_PORT=18080` 运行时不再出现 OAuth 登录页或第三方回调地址丢失端口的问题。

发布时直接上传本地新镜像到 GHCR 卡在最后的新 layer；因本次补丁的实际运行修复只涉及仓库 nginx 配置，最终采用 registry 级别重标记方式发布 `1.5.3` 与 `latest` 镜像 tag，复用 `v1.5.2` Web/API 运行内容。实际发布 digest 见 `docs/release-v1.5.3.md`。
