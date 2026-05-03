# v1.5.1 issue #10 验收记录

## 1. 验收目标

- GitHub issue: https://github.com/wodenwang/bpmt-lite/issues/10
- 入口：`/flow/CommonFlowAction/taskList.shtml`
- 数据库：完整库 `bpmt`
- 目标：点击“查看”和“处理”后不再出现 `_ORD_ID=null`，并进入真实工作流查看/办理页面。

## 2. 复现环境

- Compose project: `bpmt-v151-issue10`
- Web image: `ghcr.io/wodenwang/bpmt-lite:1.5.0`
- API image: `ghcr.io/wodenwang/bpmt-lite-api:1.5.0`
- Database: 完整库 `bpmt`，由 `database/bpmt.sql.gz` 解压到临时目录 `db/init/bpmt.sql` 初始化；本次导入后 `information_schema.tables` 统计为 380 张表。
- Browser base URL: `http://127.0.0.1:18080`
- Login:
  - `admin/admin`：用于平台入口和系统状态处理；登录后 `/flow/CommonFlowAction/taskList.shtml` 当前无待办。
  - `zhangzongcai/123`：用于点击第一条真实待办；登录后 `/flow/CommonFlowAction/taskList.shtml` 显示 1 条待办。

补充说明：

- 本机已有默认 `bpmt-*` 容器在运行。为避免影响既有运行态，本次使用临时目录启动 compose，并仅在临时副本中把固定 `container_name` 改为 `bpmt-v151-issue10-*`。
- 临时端口 `18080` 下，登录页最初因 nginx `Host` 头不带端口导致 AJAX 访问 `http://127.0.0.1/frame/LoginAction/login.shtml` 触发 CORS。仅对临时副本 nginx 配置调整 `proxy_set_header Host $http_host;` 后继续复现，不修改仓库文件。
- 业务用户登录前，平台处于维护状态，非超级用户登录提示 `系统维护中,暂停用户登陆.`。使用 `admin/admin` 访问 `/development/SystemAction/pausePlatform.shtml?pause=0` 将本次临时 runtime 切回运行中后，`zhangzongcai/123` 可登录。

## 3. 修复前复现记录

- Task ID: `b26481fc-42d3-11f1-b6d7-de29797a6eb7`
- Process instance ID: `b25dcb39-42d3-11f1-b6d7-de29797a6eb7`
- Business key: `FNBW2604001`
- Process definition: `FIN_BORROW:2:a4340014-42d3-11f1-b6d7-de29797a6eb7`
- Task name / assignee: `审核` / `zhangzongcai`
- History table: `FIN_BORROW_HI`
- History `ORD_ID`: `FNBW2604001`
- 点击“查看”网络请求:
  - 浏览器点击第一条待办“查看”按钮后，页面未发出有效 `detail.shtml`、`.view` 或包含 `_ORD_ID` 的跳转请求。
  - 控制台错误：`TypeError: Core.fn(...) is not a function`，位置 `http://127.0.0.1:18080/flow/CommonFlowAction/taskList.shtml:167:34`。
  - 当前页面仍停留在 `http://127.0.0.1:18080/flow/CommonFlowAction/taskList.shtml`，网络记录中只看到既有 `POST /flow/CommonFlowAction/getTaskCount.shtml => 200`。
- 点击“处理”网络请求:
  - 浏览器点击第一条待办“处理”按钮后，页面同样未发出有效 `form.shtml`、`.view` 或包含 `_ORD_ID` 的跳转请求。
  - 控制台错误：`TypeError: Core.fn(...) is not a function`，位置 `http://127.0.0.1:18080/flow/CommonFlowAction/taskList.shtml:172:32`。
  - 当前页面仍停留在 `http://127.0.0.1:18080/flow/CommonFlowAction/taskList.shtml`。
- 修复前结果:
  - 完整库存在真实待办，且该待办的 `BUSINESS_KEY_` 与历史表 `ORD_ID` 均为非空 `FNBW2604001`。
  - 直连 `/flow/CommonFlowAction/taskList.shtml` 后点击“查看”和“处理”在前端调用阶段被 `Core.fn(...) is not a function` 阻断，因此本轮未生成 `_ORD_ID=null` 的网络请求，也未进入真实工作流查看/办理页面。
  - 该结果不能判定 issue #10 已修复，只说明当前修复前直连入口首先暴露了按钮 handler 缺失或上下文依赖问题，后续修复仍需覆盖 `_ORD_ID=null` 防御与真实跳转验收。

## 4. 根因

本任务只做修复前复现，不修改业务代码。当前可确认的根因线索如下：

- `task_list.jsp` 页面按钮脚本调用 `Core.fn($zone, 'invokeDetail')(id)` 和 `Core.fn($zone, 'invokeTask')(id)`。
- 直连 `/flow/CommonFlowAction/taskList.shtml` 时，当前页面上下文没有提供可调用的 `invokeDetail` / `invokeTask` handler，导致点击阶段抛出 `Core.fn(...) is not a function`。
- 数据侧同一任务的 `ACT_RU_EXECUTION.BUSINESS_KEY_` 和 `FIN_BORROW_HI.ORD_ID` 都是 `FNBW2604001`，本次未观察到业务主键本身为空。
- issue #10 报告的 `_ORD_ID=null` 是否还会在完整 UI 链路或后续 `CommonFlowAction.detail/form` redirect 阶段出现，需要在修复阶段继续实点确认。

## 5. 修复点

待后续修复任务补充。本任务不编辑业务代码。

## 6. 修复后验收记录

- 点击“查看”结果: 待修复后补充。
- 点击“处理”结果: 待修复后补充。
- `_ORD_ID=null` 检查: 待修复后补充。
- 页面状态: 待修复后补充。

## 7. v1.5.0 基线回归

- `/`: `HTTP/1.1 200 OK`
- `/ueditor/`: `HTTP/1.1 200 OK`
- `/api/docs/`: `HTTP/1.1 200 OK`
- `/api/openapi.json`: `HTTP/1.1 200 OK`
- `/oauth/authorize`: `HTTP/1.1 200 OK`，未登录时返回 BPMT 登录页。

## 8. SQL 证据

```sql
select t.ID_ as TASK_ID, t.PROC_INST_ID_, e.BUSINESS_KEY_, e.PROC_DEF_ID_, t.NAME_, t.ASSIGNEE_
from ACT_RU_TASK t
left join ACT_RU_EXECUTION e on e.PROC_INST_ID_ = t.PROC_INST_ID_ and e.PARENT_ID_ is null
where t.ID_ = 'b26481fc-42d3-11f1-b6d7-de29797a6eb7';
```

结果：

| TASK_ID | PROC_INST_ID_ | BUSINESS_KEY_ | PROC_DEF_ID_ | NAME_ | ASSIGNEE_ |
| --- | --- | --- | --- | --- | --- |
| `b26481fc-42d3-11f1-b6d7-de29797a6eb7` | `b25dcb39-42d3-11f1-b6d7-de29797a6eb7` | `FNBW2604001` | `FIN_BORROW:2:a4340014-42d3-11f1-b6d7-de29797a6eb7` | `审核` | `zhangzongcai` |

```sql
select NAME_, TEXT_
from ACT_RU_VARIABLE
where PROC_INST_ID_ in (
  select PROC_INST_ID_ from ACT_RU_TASK where ID_='b26481fc-42d3-11f1-b6d7-de29797a6eb7'
)
and NAME_ in ('_ORDER_HISTORY_TABLE_NAME','_ORDER_TABLE_NAME');
```

结果：

| NAME_ | TEXT_ |
| --- | --- |
| `_ORDER_HISTORY_TABLE_NAME` | `FIN_BORROW_HI` |
| `_ORDER_TABLE_NAME` | `FIN_BORROW` |

```sql
select TASK_ID, ORD_ID, TASK_BEGIN_DATE, TASK_END_DATE
from FIN_BORROW_HI
where TASK_ID='b26481fc-42d3-11f1-b6d7-de29797a6eb7'
order by ID desc
limit 5;
```

结果：

| TASK_ID | ORD_ID | TASK_BEGIN_DATE | TASK_END_DATE |
| --- | --- | --- | --- |
| `b26481fc-42d3-11f1-b6d7-de29797a6eb7` | `FNBW2604001` | `2026-04-28 15:27:22` | `NULL` |
