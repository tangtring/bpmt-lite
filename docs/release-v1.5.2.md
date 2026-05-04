# v1.5.2 发布记录

## 发布定位

`v1.5.2` 是基于 `v1.5.1` 的 OAuth 体验补丁。`v1.5.1` 的工作流待办跳转修复继续保留，`v1.5.0` 的外部系统 OAuth 登录能力继续保留。

## 变更内容

- OAuth authorize 在浏览器已有 BPMT 登录态时继续复用当前用户。
- 当前用户没有目标第三方系统权限时，显示 BPMT 内部提示页。
- 用户可选择退出当前账号并重新登录其他 BPMT 账号。
- 用户可选择取消并返回第三方系统 `access_denied`。
- README 增加 OAuth demo 项目链接：https://github.com/wodenwang/bpmt-oauth-demo

## 不变范围

- 不新增 OAuth 协议端点。
- 不修改 `bpmt-api` 业务接口。
- 不修改 OAuth 表结构。
- 不修改 `clientSecret` 的 hash-only 保存和重置规则。

## 验收摘要

发布前补充 `docs/v1.5.2/oauth-session-switch-acceptance.md` 中的最终运行记录。
