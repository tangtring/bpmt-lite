# v1.6.0 HTTPS 验收记录

## 验收目标

v1.6.0 验证 `bpmt-lite` 在内置 nginx HTTPS 入口和可信上游代理语义下可正常运行。验收覆盖 Web、UEditor、API、OAuth、H5、前端 mixed content 和既有补丁基线。

## 静态门禁

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| `scripts/verify-repo.sh` | PASS | `OK: repository hygiene checks passed` |
| `docker compose config` | PASS | 默认 compose config 可生成，且默认不发布 443 |
| Java 8 全仓编译 | PASS | `mvn -s settings.local.xml -DskipTests compile` 返回 `BUILD SUCCESS` |
| API 单测 | PASS | `mvn -s settings.local.xml -pl api test`，`Tests run: 39, Failures: 0, Errors: 0` |
| OAuth/HTTPS 目标单测 | PASS | `ActionsForwardedUrlTest`、`OAuthActionTest`、`HttpsStaticResourceTest` 通过 |
| 本地 Web/API 镜像构建 | PASS | `scripts/build-image.sh` 与 `scripts/build-api-image.sh` 均通过，生成 `1.6.0` 本地镜像 |

## HTTPS runtime

计划命令：

```bash
BPMT_HTTPS_ENABLED=1 BPMT_HTTPS_PORT=18443 BPMT_HTTP_PORT=18080 \
  docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
```

| 入口 | 期望 | 结果 |
| --- | --- | --- |
| `https://127.0.0.1:18443/` | 200 | PASS，`curl -k -fsSI` 返回 `HTTP/1.1 200 OK` |
| `https://127.0.0.1:18443/ueditor/` | 200 | PASS，`curl -k -fsSI` 返回 `HTTP/1.1 200 OK` |
| `https://127.0.0.1:18443/api/docs/` | 200 | PASS，`curl -k -fsSI` 返回 `HTTP/1.1 200 OK` |
| `https://127.0.0.1:18443/api/openapi.json` | 200 | PASS，`curl -k -fsSI` 返回 `HTTP/1.1 200 OK` |
| `http://127.0.0.1:18080/` | 301 到 HTTPS | PASS，`Location: https://127.0.0.1:18443/` |
| `bpmt_min` 表数量 | 176 | PASS，`information_schema.tables` 返回 `176` |
| `bpmt` 表数量 | 380 | PASS，完整库运行时返回 `380` |

## 浏览器验收

| 路径 | 期望 | 结果 |
| --- | --- | --- |
| 桌面登录页 | 无 mixed content 阻断 | PASS，`https://127.0.0.1:18443/login.jsp` 返回 200，console 无 mixed content |
| H5 登录页 | 无 mixed content 阻断 | PASS，`https://127.0.0.1:18443/login.jsp?_action_mode=h5` 返回 200，console 无 mixed content |
| 首页和菜单 | 登录后可浏览 | PASS，`admin/admin` 登录后进入 `J_DpS0eJL9X.xhtml`，首页和菜单文本可见 |
| H5 代表业务路径 | 可浏览 | PASS，H5 登录页渲染；运行主路径未再请求 HTTP CDN 阻断资源 |
| `/oauth/authorize` | HTTPS 登录与回跳正确 | PASS，无效 `demo-client` 返回 OAuth 错误页，说明 HTTPS 下 filter/action 正常接管 |
| `/api/docs/` | HTTPS 页面可读 | PASS，页面标题 `BPMT Lite API Docs` |
| HTTP CDN/mixed-content 扫描 | 无阻断 | PASS，浏览器未出现 `http://apps.bdimg.com`、`http://cdn.bootcss.com`、`http://res.wx.qq.com` 失败请求 |

## API HTTPS smoke

计划命令：

```bash
BPMT_API_BASE_URL=https://127.0.0.1:18443/api BPMT_API_CURL_INSECURE=1 scripts/smoke-api.sh
```

结果：PASS。最小库和完整库运行时均输出：

```text
API smoke passed: https://127.0.0.1:18443/api
```

## 继承回归

| 基线 | 期望 | 结果 |
| --- | --- | --- |
| v1.5.1 issue #10 | 工作流待办“查看/处理”无 `_ORD_ID=null` | PASS，完整库 `zhangzongcai/123` 点击“查看”和“处理”，两条 `.view` 请求均包含 `_ORD_ID=FNBW2604001`，未出现 `_ORD_ID=null` |
| v1.5.2 OAuth 登录态切换 | 无权限提示、取消、切换账号可用 | PARTIAL，本轮验证 HTTPS 下 `/oauth/authorize` 正常进入 OAuth action；完整交互沿用 v1.5.2 验收，发布前如改 OAuth 交互需复测 |
| v1.5.3 非标准端口 | OAuth URL 保留端口 | PASS，HTTPS 运行在 `18443`，浏览器和工作流跳转请求均保留 `https://127.0.0.1:18443` |
| v1.5.4 multi-arch | Web/API manifest 包含 amd64 和 arm64 | 待发布，multi-arch manifest 在 release closure 使用 `scripts/build-multiarch-images.sh` 验证 |

## issue #10 HTTPS 完整库证据

完整库运行时：

- Compose project: `bpmt-v160-full`
- Web image: `ghcr.io/wodenwang/bpmt-lite:1.6.0`
- API image: `ghcr.io/wodenwang/bpmt-lite-api:1.6.0`
- Browser base URL: `https://127.0.0.1:18443`
- Login: `zhangzongcai/123`

点击“查看”网络请求：

```text
POST https://127.0.0.1:18443/flow/CommonFlowAction/detail.shtml
GET  https://127.0.0.1:18443/1iI5xylQL9X.view?...&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null
```

点击“处理”网络请求：

```text
POST https://127.0.0.1:18443/flow/CommonFlowAction/form.shtml
GET  https://127.0.0.1:18443/1iI5xylQL9X.view?...&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null
```

浏览器结果：

- 查看窗口标题包含 `员工借款[FNBW2604001]:审核`。
- 处理窗口标题包含 `员工借款[FNBW2604001]:审核`。
- console error 为空，page error 为空。
