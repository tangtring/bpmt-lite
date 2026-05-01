# v1.3.0 H5 业务视图验收清单

## 范围修正

v1.3.0 的移动端重点不是后台管理模块的“功能设置”域，而是业务视图在 H5 下的可浏览、可操作能力。

本轮按 `com.riversoft.platform.web.view.annotation.View` 扫描出的视图类型验收：

| 视图类型 | 数量 | v1.3.0 范围 |
| --- | ---: | --- |
| `dyn` | 52 | 动态表列表、详情、新建、编辑、删除闭环 |
| `flowbasic` | 35 | 工作流入口、列表、详情、表单、普通办理主路径 |
| `rep_list` | 21 | 报表列表、查询、详情、下载兼容 |
| `note` | 1 | 公告列表和详情 |
| `viewer` | 32 | 兼容降级：HTML/消息可读，下载类不破 |

## 运行基线

当前优先使用完整库 `bpmt` 验收，保证业务数据覆盖更充分。

- 临时 Web 地址：`http://127.0.0.1:18080/`
- 临时数据库：`bpmt`
- 管理账号：`admin/admin`
- 移动视口建议：`390x844`
- 强制 H5 参数：`_action_mode=h5`

## 通用入口验收

| 项目 | 验收动作 | 预期 |
| --- | --- | --- |
| 登录页 | 打开 `/login.jsp?_action_mode=h5` | 返回 200，无阻断性 JS 错误 |
| 静态资源 | 检查 `/css/amazeui.min.css`、`/js/amazeui.min.js`、`/h5/assets/bpmt-h5.css`、`/h5/assets/bpmt-h5.js` | 返回 200 |
| 登录 | 使用 `admin/admin` 登录 | 返回成功并进入 H5 首页 |
| 首页 | 打开 `/?_action_mode=h5` | 可见首页、菜单、首页面板 |
| 菜单链接 | 从 H5 菜单点击业务视图入口 | 链接不出现 `http://frame`、`//frame/FrameAction` 等异常 URL |

## 动态表 `dyn`

动态表必须完成移动端增删改查闭环。验收时每条 URL 至少覆盖列表、详情；选 1 条可写业务做新建、编辑、删除。

| 业务 | URL | 验收动作 |
| --- | --- | --- |
| OA-事项资料库 | `/dyn/Akf3zTHgJL9XAction/list.shtml?_action_mode=h5` | 列表、搜索/分页、详情、新建、编辑、删除 |
| OA-公告管理 | `/dyn/AeIG7qChJL9XAction/list.shtml?_action_mode=h5` | 列表、详情、表单字段展示 |
| 人力-员工-档案资料 | `/dyn/AqNgdqEeJL9XAction/list.shtml?_action_mode=h5` | 列表、详情、多字段可读 |
| 产品-BOM表 | `/dyn/AYP7bCBhzM9XAction/list.shtml?_action_mode=h5` | 列表、详情、子表/关联字段降级可读 |
| 财务-费用报销-快捷 | `/dyn/AbcJ76of2M9XAction/list.shtml?_action_mode=h5` | 列表、详情、金额/日期字段可读 |

动态表通过标准：

- 列表在移动宽度下不横向撑破页面。
- 查询条件、分页、空状态可用。
- 详情字段按移动端纵向信息块展示。
- 表单输入控件可点击、可输入、可提交。
- 提交成功/失败有明确反馈。
- 删除操作有确认，不因弹窗库缺失导致 JS 中断。

## 工作流 `flowbasic`

工作流必须覆盖入口、列表、详情、表单和普通办理主路径。涉及业务状态变更的提交只在可控测试数据上做。

| 业务 | URL | 验收动作 |
| --- | --- | --- |
| 财务-收支登记 | `/flow/view/AQPyBgISJL9XAction/main.shtml?_action_mode=h5` | 首页入口、个人待办/全部列表、详情 |
| 财务-费用报销 | `/flow/view/A8060m_rKL9XAction/main.shtml?_action_mode=h5` | 列表、详情、表单、普通办理按钮 |
| OA-通用事项申请 | `/flow/view/AELy2gFhJL9XAction/main.shtml?_action_mode=h5` | 发起/详情/意见输入主路径 |
| 人力-薪酬-请假申请 | `/flow/view/AkzFiJIdJL9XAction/main.shtml?_action_mode=h5` | 表单字段、日期字段、提交反馈 |
| 销售-销售订单 | `/flow/view/AKZyC3MqLL9XAction/main.shtml?_action_mode=h5` | 长字段、多业务字段移动端可读 |

工作流通过标准：

- `main` 不落到桌面布局。
- `portal_person`、`portal_all`、`list` 可在移动端浏览。
- `detail` 和 `form` 字段纵向可读。
- 办理意见输入可用。
- 普通流转按钮可点击，有提交反馈。
- 暂不要求流程设计器、配置页移动端适配。

## 报表 `rep_list`

报表必须覆盖列表、查询、详情和下载兼容。

| 业务 | URL | 验收动作 |
| --- | --- | --- |
| OA-个人经办订单 | `/report/AjgOEUDhJL9XAction/list.shtml?_action_mode=h5` | 列表、查询、详情 |
| 人力-员工-通讯录 | `/report/Aw6$FtUgJL9XAction/list.shtml?_action_mode=h5` | 列表、详情、横向字段降级 |
| 人力-考勤记录表 | `/report/APdq87aCaM9XAction/list.shtml?_action_mode=h5` | 查询、分页、详情 |
| 进销存-销售汇总表 | `/report/ATBN7KS7OL9XAction/list.shtml?_action_mode=h5` | 汇总字段、详情或下载入口兼容 |
| 库存管理-统计报表 | `/report/A6f585YnLL9XAction/list.shtml?_action_mode=h5` | 数值字段、分页、空状态 |

报表通过标准：

- 列表字段在移动端可扫描。
- 查询区不遮挡内容。
- 详情页可读。
- 下载按钮保留原能力；移动端不支持预览时必须保持可点击或明确提示。

## 公告 `note`

| 业务 | URL | 验收动作 |
| --- | --- | --- |
| OA-系统公告 | `/Fn7MNvjJL9X.view?_action_mode=h5` | 列表、详情 |

公告通过标准：

- `.view` 路由能正确进入 H5 页面。
- 列表和详情可读。
- 无登录访问类型的兼容行为不被破坏。

## Viewer `viewer`

Viewer 是输出型视图，本轮不作为完整移动端页面框架重构对象，只做兼容降级。

| 业务 | URL | 验收动作 |
| --- | --- | --- |
| 客户-本月销售表 | `/CX2ZbyIJM9X.view?_action_mode=h5` | HTML 输出可读 |
| 工资发放趋势图 | `/Dsi9dkbLL9X.view?_action_mode=h5` | 图表/HTML 输出不阻断 |
| 财务-本月现金流 | `/-fJjcg8OL9X.view?_action_mode=h5` | 表格输出可读 |
| 导出-财务支出流水导出 | `/EacwCbS3N9X.view?_action_mode=h5` | 下载类输出不 500 |
| 采购-采购订单报表导出 | `/9D1VKAfMM9X.view?_action_mode=h5` | 下载类输出不 500 |

Viewer 通过标准：

- HTML/TEXT/MSG 类结果在移动端可读。
- WORD/EXCEL/PDF 类结果保持下载或返回明确响应，不因 H5 包装导致 500。
- 不要求本轮把所有 Viewer 结果改造成移动端组件。

## C 级高频主路径冒烟

本轮 C 级冒烟只选两条主路径，不覆盖全部业务提交。

1. 动态表 CRUD 闭环：
   - 入口：`/dyn/Akf3zTHgJL9XAction/list.shtml?_action_mode=h5`
   - 动作：列表 -> 新建 -> 填写基础字段 -> 保存 -> 详情/列表确认 -> 编辑 -> 删除测试数据
2. 工作流普通办理闭环：
   - 入口：`/flow/view/AQPyBgISJL9XAction/main.shtml?_action_mode=h5`
   - 动作：列表/发起 -> 表单 -> 填写意见 -> 点击普通办理按钮 -> 返回成功或状态变化

## 每轮记录

- Docker compose 启动方式。
- 数据库：`bpmt` 或 `bpmt_min`。
- 请求 URL。
- HTTP 状态。
- 浏览器控制台关键错误。
- 移动视口截图或文字记录。
- 是否涉及写入数据；涉及写入时记录测试数据标识。

## 自动化缺口

- 当前仓库没有可直接复用的 servlet request mock 测试基线；`Actions.isMobile()` 的短 User-Agent 防御通过代码审查和运行期 H5 smoke 覆盖。
- H5 业务视图以 JSP 和历史动态配置驱动为主，本轮优先用完整库业务 URL 做浏览器验收，后续再补更细的自动化回归。

## 2026-05-01 完整库基线记录

- 运行方式：临时 compose 项目 `bpmt-h5-full`
- Web：`http://127.0.0.1:18080/`
- 数据库：`bpmt`
- 账号：`admin/admin`
- 视图数量：`dyn=52`、`flowbasic=35`、`rep_list=21`、`note=1`、`viewer=32`
- 移动视口：`390x844`，iPhone Safari UA

### 已确认阻断问题

| 问题 | 复现 URL | 处理 |
| --- | --- | --- |
| 动态业务 Action 直连 500 | `/dyn/Akf3zTHgJL9XAction/list.shtml?_action_mode=h5` | 已修复：直接访问 `dyn`、`flowbasic`、`rep_list` 的 `A...Action` URL 时，按历史规则补生成运行期 Action 类 |
| FontAwesome 字体 404 | 所有使用 AmazeUI 图标的 H5 业务页 | 已补齐：从稳定运行时参考目录恢复 `/fonts/fontawesome-webfont.{woff2,woff,ttf}` |

### 当前已通过入口

| 视图 | URL | 当前结果 |
| --- | --- | --- |
| `dyn` | `/dyn/Akf3zTHgJL9XAction/list.shtml?_action_mode=h5` | 200，可见“公司资料”列表和查询区 |
| `flowbasic` | `/flow/view/AQPyBgISJL9XAction/main.shtml?_action_mode=h5` | 200，可见“收入登记”列表和查询区 |
| `rep_list` | `/report/AjgOEUDhJL9XAction/list.shtml?_action_mode=h5` | 200，可见“个人经办”列表和查询区 |

### 待继续处理

- 当前列表页仍以查询区优先展开，业务列表信息密度和移动端操作区仍需按计划继续重构。
- 动态表 CRUD 写入闭环、工作流普通办理闭环尚未执行。
- `note`、`viewer`、完整 URL 矩阵尚未逐条完成。
