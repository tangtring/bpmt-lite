# v1.3.0 H5 验收清单

## 必达范围

- `/login.jsp?_action_mode=h5` 返回 200。
- 登录页关键 CSS/JS 全部返回 200。
- 登录页无阻断性 JavaScript 错误。
- `admin/admin` 可登录。
- 登录后 `/` 在移动视口可看到首页、菜单、首页面板。
- 流程、动态表、报表至少各一条列表和详情路径可浏览。

## C 级冒烟

- 动态表单：进入表单，填写基础文本、下拉、日期字段，提交保存。
- 流程待办：进入待办详情，填写意见，提交普通流转动作。

## 每轮记录

- Docker compose 启动方式。
- 数据库：`bpmt` 或 `bpmt_min`。
- 请求 URL。
- HTTP 状态。
- 浏览器控制台关键错误。
- 移动视口截图或文字记录。

## 自动化缺口

- 当前仓库没有可直接复用的 servlet request mock 测试基线；`Actions.isMobile()` 的短 User-Agent 防御先通过代码审查和运行期 H5 smoke 覆盖。

## 2026-05-01 本地 smoke 记录

- 分支：`codex/v1.3.0-h5-repair`
- 镜像构建：`scripts/build-image.sh` 通过，生成本地镜像 `ghcr.io/wodenwang/bpmt-lite:1.2.0`。
- 运行方式：临时 compose 项目 `bpmt-h5-smoke`，目录 `/tmp/bpmt-h5-smoke-runtime`，Web 端口 `18080`，数据库 `bpmt_min`。
- 说明：默认 `docker compose` 的 `8080` 环境已存在旧 `db/data`，本轮未删除用户本地数据；H5 验收使用临时 `bpmt_min` 环境从零初始化。

### B 级入口验收

| 项目 | 结果 |
| --- | --- |
| `/login.jsp?_action_mode=h5` | 200 |
| `/css/amazeui.min.css` | 200 |
| `/js/amazeui.min.js` | 200 |
| `/h5/assets/bpmt-h5.css` | 200 |
| `/h5/assets/bpmt-h5.js` | 200 |
| `/js/ws-wxui.js` | 200 |
| `/ueditor/` | 200 |
| `admin/admin` 登录 | `{"flag":true}` |
| 登录后 `/?_action_mode=h5` | 200，可见“功能设置”“菜单”“首页面板” |
| 菜单页 `/frame/FrameAction/menu.shtml?domain=manage&_action_mode=h5` | 200，可见 H5 菜单 |
| 菜单 URL 检查 | 未发现 `http://frame`、`//frame/FrameAction` 或协议相对异常链接 |

移动端浏览器验收使用 `390x844`、iPhone User-Agent：

- 登录页可输入 `admin/admin` 并进入 H5 frame 首页。
- 首页可进入菜单页。
- 菜单页“首页”链接归一化为 `http://127.0.0.1:18080/frame/FrameAction/panel.shtml?...`。
- 浏览器 console 未发现 error/warn。

### C 级主路径冒烟

按本轮约定只选择高频主路径冒烟，不覆盖完整业务提交：

- “数据字典”：`/manager/db/DbAction/index.shtml?_action_mode=h5&_frame_type=1` 可打开，页面渲染出“字典数据设置”“刷新”“新建类别”“数据分组”。
- “流程设置”：`/flow/PdAction/index.shtml?_action_mode=h5&_frame_type=1` 可打开，页面渲染出“流程定义配置”“快捷设置”“流程分类列表”。
- 两条路径移动端浏览器 console 均无 error/warn。
