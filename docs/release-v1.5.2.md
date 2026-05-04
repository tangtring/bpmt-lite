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

2026-05-04 已完成 `docs/v1.5.2/oauth-session-switch-acceptance.md` 记录的发布前验收：

- OAuth 单测 24/24 通过，完整 OAuth 测试集合 40/40 通过。
- Java 8 全仓编译通过，`docker compose config` 通过。
- 本地镜像构建通过：`ghcr.io/wodenwang/bpmt-lite:1.5.2`、`ghcr.io/wodenwang/bpmt-lite-api:1.5.2`。
- 临时完整库运行时 `bpmt-v152` 启动通过，完整库表数 380。
- `/`、`/ueditor/`、`/api/docs/`、`/api/openapi.json`、`/oauth/authorize` 均返回 200，`scripts/smoke-api.sh` 通过。
- OAuth 浏览器验收覆盖未登录、已有登录态有权限、已有登录态无权限提示、取消返回 `access_denied`、切换账号后重新登录并继续授权。
- `v1.5.1` issue #10 回归通过：`zhangzongcai/123` 点击待办“查看/处理”均生成真实 `_ORD_ID=FNBW2604001`，未出现 `_ORD_ID=null`。

镜像推送、Git tag 和 GitHub Release 以实际发布命令结果为准。
