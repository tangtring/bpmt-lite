# v1.5.1 发布记录

## 发布定位

`v1.5.1` 是基于 `v1.5.0` 的补丁版本。`v1.5.0` 的外部系统 OAuth 登录能力继续保留，本版本不新增 OAuth 端点、不修改 `bpmt-api` 业务接口。

## 修复内容

- 修复 GitHub issue #10：完整库 `bpmt` 中 `/flow/CommonFlowAction/taskList.shtml` 点击“查看/处理”后跳转 URL 可能出现 `_ORD_ID=null` 的问题。
- 待办任务跳转优先使用 Activiti `processInstance.businessKey` 作为订单号。
- 对旧数据或异常数据，允许从 BPMT 工作流历史表按 `TASK_ID` 解析 `ORD_ID`。
- 无法解析订单号时返回明确业务错误，不再生成 `_ORD_ID=null` 链接。

## 验收摘要

- 最终 `1.5.1` Web/API 镜像已完成完整库 `bpmt` issue #10 浏览器验收：见 `docs/v1.5.1/issue-10-acceptance.md`。
- `/`、`/ueditor/`、`/api/docs/`、`/api/openapi.json` 回归通过。
- `/oauth/authorize` 基线回归通过。
- `scripts/smoke-api.sh` 回归通过。
- OAuth 单测回归通过。
- Hazelcast Web/API 双 member 回归通过。

## 镜像列表

- `ghcr.io/wodenwang/bpmt-lite:1.5.1`
- `ghcr.io/wodenwang/bpmt-lite-api:1.5.1`
- `ghcr.io/wodenwang/bpmt-lite:latest`
- `ghcr.io/wodenwang/bpmt-lite-api:latest`
