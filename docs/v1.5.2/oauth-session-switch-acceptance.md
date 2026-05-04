# v1.5.2 OAuth 登录态切换验收记录

## 验收目标

- 第三方系统未登录时，继续由第三方系统跳转 BPMT `/oauth/authorize`。
- 浏览器已有 BPMT 登录态时，BPMT 复用当前用户，不强制显示登录页。
- 当前 BPMT 用户无目标第三方权限时，显示 BPMT 内部提示页。
- 用户可选择退出当前账号并重新登录。
- 用户可选择取消并返回第三方 `access_denied`。
- `clientSecret` 仍只展示一次、只能重置、数据库只保存 hash。

## 单测验收

待实现后记录命令和结果：

```bash
mvn -s settings.local.xml -pl platform -Dtest=OAuthActionTest,OAuthLoginReturnTest -DfailIfNoTests=false test
```

## 运行验收计划

| 项目 | 结果 |
| --- | --- |
| `/` | 期望返回 200 |
| `/ueditor/` | 期望返回 200 |
| `/api/docs/` | 期望返回 200 |
| `/api/openapi.json` | 期望返回 200 |
| `/oauth/authorize` 未登录 | 期望进入 BPMT 登录页 |
| `/oauth/authorize` 已登录有权限 | 期望回跳可信 `redirect_uri` 并携带 `code` |
| `/oauth/authorize` 已登录无权限 | 期望显示 BPMT 无权限提示页 |
| 无权限页取消返回第三方 | 期望回跳可信 `redirect_uri` 并携带 `error=access_denied` |
| 无权限页退出换账号 | 期望进入 BPMT 登录页，重新登录后回到原 authorize |
| issue #10 查看/处理回归 | 期望不出现 `_ORD_ID=null` |

最终运行证据将在发布验收阶段追加到本记录。
