# v1.6.0 HTTPS 验收记录

## 验收目标

v1.6.0 验证 `bpmt-lite` 在内置 nginx HTTPS 入口和可信上游代理语义下可正常运行。验收覆盖 Web、UEditor、API、OAuth、H5、前端 mixed content 和既有补丁基线。

## 静态门禁

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| `scripts/verify-repo.sh` | 未执行 | 待发布前补充 |
| `docker compose config` | 未执行 | 待发布前补充 |
| Java 8 全仓编译 | 未执行 | 待发布前补充 |
| API 单测 | 未执行 | 待发布前补充 |
| OAuth/HTTPS 目标单测 | 未执行 | 待发布前补充 |

## HTTPS runtime

计划命令：

```bash
BPMT_HTTPS_ENABLED=1 BPMT_HTTPS_PORT=18443 BPMT_HTTP_PORT=18080 \
  docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
```

| 入口 | 期望 | 结果 |
| --- | --- | --- |
| `https://127.0.0.1:18443/` | 200 | 未执行 |
| `https://127.0.0.1:18443/ueditor/` | 200 | 未执行 |
| `https://127.0.0.1:18443/api/docs/` | 200 | 未执行 |
| `https://127.0.0.1:18443/api/openapi.json` | 200 | 未执行 |
| `http://127.0.0.1:18080/` | 301 到 HTTPS | 未执行 |

## 浏览器验收

| 路径 | 期望 | 结果 |
| --- | --- | --- |
| 桌面登录页 | 无 mixed content 阻断 | 未执行 |
| H5 登录页 | 无 mixed content 阻断 | 未执行 |
| 首页和菜单 | 登录后可浏览 | 未执行 |
| H5 代表业务路径 | 可浏览 | 未执行 |
| `/oauth/authorize` | HTTPS 登录与回跳正确 | 未执行 |
| `/api/docs/` | HTTPS 页面可读 | 未执行 |

## API HTTPS smoke

计划命令：

```bash
BPMT_API_BASE_URL=https://127.0.0.1:18443/api BPMT_API_CURL_INSECURE=1 scripts/smoke-api.sh
```

结果：未执行。

## 继承回归

| 基线 | 期望 | 结果 |
| --- | --- | --- |
| v1.5.1 issue #10 | 工作流待办“查看/处理”无 `_ORD_ID=null` | 未执行 |
| v1.5.2 OAuth 登录态切换 | 无权限提示、取消、切换账号可用 | 未执行 |
| v1.5.3 非标准端口 | OAuth URL 保留端口 | 未执行 |
| v1.5.4 multi-arch | Web/API manifest 包含 amd64 和 arm64 | 未执行 |
