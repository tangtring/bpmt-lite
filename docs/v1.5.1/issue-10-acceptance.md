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

本次修复范围限定在工作流待办查看/处理链路：

- `xhtml/flow/CommonFlowAction/task_list.jsp`
  - 直连 `/flow/CommonFlowAction/taskList.shtml` 时，如果父级页面没有注册 `invokeDetail` / `invokeTask`，按钮会 fallback 到本 action 的 `detail.shtml` / `form.shtml`。
  - 如果由 `taskMain.shtml` 等父级页面加载，仍优先调用父级 handler，保留原 tab / quickMode / callback 行为。
- `CommonFlowAction.detail()` / `CommonFlowAction.form()`
  - task 入参路径增加 `task == null`、`processInstance == null` 防御。
  - `ordId` 优先使用 `ProcessInstance.businessKey`。
  - `businessKey` 为空时，从流程变量 `_ORDER_HISTORY_TABLE_NAME` 指向的历史表按 `TASK_ID` 兜底查 `ORD_ID`。
  - 仍找不到订单号时抛业务异常，不再重定向到 `_ORD_ID=null`。
  - `.view` 重定向中的 `_params`、`_TASK_ID`、`_ORD_ID` query value 统一 URL encode，避免 Tomcat 7 因 raw `{}` 返回 400。
- `BaseFlowBasicAction.form()` / `BaseFlowBasicAction.detail()`
  - 目标视图收到 `_TASK_ID` 后，只在 `businessKey` 非空时覆盖已有 `FlowObject.ordId`，避免把上游兜底出的 `_ORD_ID` 覆盖为空。
- 测试：
  - `CommonFlowActionIssue10Test`
  - `BaseFlowBasicActionIssue10Test`

相关提交：

- `0c899cb fix(flow): repair task list navigation`
- `89c0b96 fix(flow): preserve task order fallback`
- `76c235b fix(flow): pass fallback order to detail`
- `3aa4184 fix(flow): encode view redirect params`

## 6. 修复后验收记录

2026-05-04 使用当前分支重新构建 Web 镜像 `ghcr.io/wodenwang/bpmt-lite:1.5.0`，并在临时完整库 compose 环境中只重建 `bpmt-v151-issue10-web` 后复测。

复测前置：

- `DB_PASSWORD=root docker compose -p bpmt-v151-issue10 -f /tmp/bpmt-v151-issue10-5vJiZM/docker-compose.yml up -d --no-deps --force-recreate bpmt-web`
- `/` 返回 `200`
- `admin/admin` 访问 `/development/SystemAction/pausePlatform.shtml?pause=0`，将临时运行态切出维护模式。
- `zhangzongcai/123` 登录后，`/flow/CommonFlowAction/taskList.shtml` 显示 1 条真实待办：
  - `TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7`
  - `ORD_ID=FNBW2604001`

点击“查看”结果：

- `GET /flow/CommonFlowAction/taskList.shtml => 200`
- `POST /flow/CommonFlowAction/detail.shtml => 302`
- `GET /1iI5xylQL9X.view?_params=%7Bdetail%3Atrue%2CtaskId%3A%27b26481fc-42d3-11f1-b6d7-de29797a6eb7%27%7D&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null => 200`
- 页面打开 `员工借款[FNBW2604001]:审核` 查看窗口，能看到基础信息、流程意见、流程信息和流程历史。
- 浏览器 console 无新增 error。

点击“处理”结果：

- `GET /flow/CommonFlowAction/taskList.shtml => 200`
- `POST /flow/CommonFlowAction/form.shtml => 302`
- `GET /1iI5xylQL9X.view?_params=%7Bform%3Atrue%2CpdKey%3A%27%27%7D&_TASK_ID=b26481fc-42d3-11f1-b6d7-de29797a6eb7&_ORD_ID=FNBW2604001&_zone=win_2&_form=null => 200`
- 页面打开 `员工借款[FNBW2604001]:审核` 处理窗口，能看到基础信息、流程意见、保存、转交、退回和确认按钮。
- 浏览器 console 无新增 error。

`_ORD_ID=null` 检查：

- “查看”与“处理”两条网络记录均不包含 `_ORD_ID=null`。
- 两条 `.view` 请求均包含 `_ORD_ID=FNBW2604001`。
- 两条 `.view` 请求均使用已编码 `_params=%7B...%7D`，不再触发 Tomcat 7 `Invalid character found in the request target`。

页面状态：

- 修复后直连 `/flow/CommonFlowAction/taskList.shtml` 不再抛 `Core.fn(...) is not a function`。
- 修复后“查看”和“处理”均进入真实工作流查看/办理页面。

## 7. v1.5.0 基线回归

2026-05-04 使用同一临时完整库环境回归：

| 项目 | 结果 |
| --- | --- |
| `/` | `200` |
| `/ueditor/` | `200` |
| `/api/docs/` | `200` |
| `/api/openapi.json` | `200` |
| `/oauth/authorize` | `200` |
| `/oauth/authorize?response_type=code&client_id=client-a&redirect_uri=http%3A%2F%2Fclient.example%2Fcallback&state=s-1` | `200`，返回 OAuth 错误页，说明 OAuth filter/action 仍正常接管请求 |
| `scripts/smoke-api.sh` with `BPMT_API_BASE_URL=http://127.0.0.1:18080/api` | `API smoke passed` |
| OAuth 单测 | `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0` |
| 完整库表数 | `380` |
| Hazelcast Web/API | Web 日志显示 `Members [2]`，包含 `bpmt-api` 与 `bpmt-web` |

回归命令：

```bash
for path in / /ueditor/ /api/docs/ /api/openapi.json /oauth/authorize; do
  /usr/bin/curl -s -o /tmp/body -w '%{http_code}\n' "http://127.0.0.1:18080$path"
done

PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin \
BPMT_API_BASE_URL=http://127.0.0.1:18080/api \
scripts/smoke-api.sh

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH"
mvn -s settings.local.xml -pl platform \
  -Dtest=OAuthActionTest,OAuthServiceTest,OAuthSecurityTest,OAuthHbmMappingTest,OAuthLoginReturnTest,OAuthDatabaseInitSqlTest \
  test
```

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
