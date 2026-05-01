# v1.3.0 发布记录

`v1.3.0` 是保守 H5 修复版本。版本目标不是重写移动端 UI，也不替换 AmazeUI，而是在保留原 H5 页面结构的前提下修复移动端阻断问题。

## 发布内容

- 恢复 H5 登录、首页、菜单、首页面板和本地 AmazeUI 资源加载。
- 修复 `dyn`、`flowbasic`、`rep_list` 业务视图 Action 直连时动态 Action 类未生成导致的 500。
- 恢复 FontAwesome 字体资源，避免 AmazeUI 图标字体 404。
- 保留原 AmazeUI H5 业务页面结构，撤回新增的 `bpmt-h5-*` 结构类和全局资源引用。
- 修复公告详情返回时丢失 `_action_mode=h5` 的问题。
- 修复登录态过期后 H5 登录成功不回到原目标 URL 的问题。
- 修复 `.xhtml?_action_mode=h5` 被错误兜底到 H5 frame 的问题，`.xhtml` 继续按桌面入口处理。
- 修复 H5 工作流审批意见中文乱码：工作流表单改回 POST，Action 入口在读取参数前设置 UTF-8。

## 验收范围

- `dyn` 动态表代表 URL。
- `flowbasic` 工作流代表 URL，包含“请假”普通办理主路径。
- `rep_list` 报表代表 URL。
- `note` 公告列表和详情。
- `viewer` HTML/文本类兼容降级与下载类不破坏。

详细 URL 和记录见 [docs/v1.3.0/h5-acceptance.md](v1.3.0/h5-acceptance.md)。

## 发布产物

- Git tag：`v1.3.0`
- Web 镜像：`ghcr.io/wodenwang/bpmt-lite:1.3.0`
- 同步镜像：`ghcr.io/wodenwang/bpmt-lite:latest`

