# v1.2.0 迭代规划

`v1.2.0` 的目标是收口 `v1.1.0` 发布后暴露的问题，并把初始化数据库、文档、协作方式、品牌信息整理成更适合公开协作的形态。本版本仍然遵守 `bpmt-lite` 的边界：不升级技术栈、不重写业务功能、不扩展新业务能力。

## 版本目标

- 修复 `v1.1.0` 之后 GitHub issue 中确认的缺陷。
- 修复 Docker 镜像在 Apple Silicon 上显示 `AMD64` 的问题，并补齐流程图中文字体。
- 形成 `bpmt` 和 `bpmt_min` 两套初始化数据库选择，允许在同一个 MariaDB 实例中共存。
- 重构 README，让没有 Java、Maven、旧 BPMT 背景的新用户也能按最短路径启动。
- 初始化面向 Codex agent 的团队开发说明，明确协作模式、范围控制和交接规则。
- 清理不适合公开提交的 IDE 本地配置。
- 更换默认 logo 和默认 copyright，去掉 `Riversoft` 展示字样。
- 为未来 MIT 声明做准备，主要作者记录为 `wodenwang` 和 `borballzhai`。

## GitHub issue 清单

截至 2026-04-28，GitHub open issue 清单如下：

| Issue | 标题 | 规划处理 |
| --- | --- | --- |
| [#6](https://github.com/wodenwang/bpmt-lite/issues/6) | 项目 maven setting example 文件中出现写死的本地路径 | 已移除 `settings.example.xml` 中的本机 Maven 仓库路径，并移除 Aliyun 镜像配置，示例配置只保留可公开复用的 Central 和 JumpMind。 |
| [#7](https://github.com/wodenwang/bpmt-lite/issues/7) | 工作流设计模块中，无法打开 activity 编辑器 | 已通过 `ModelerServiceServlet` 恢复 Activiti Modeler 所需的 `/service/*` 兼容端点，并完成打开、保存、关闭路径验证。 |

## 初始化数据库规划

`v1.2.0` 公开仓库后计划提供两份 SQL：

| 数据库 | SQL 文件 | 说明 |
| --- | --- | --- |
| `bpmt_min` | `database/bpmt-min.sql` | 最小版初始化库，继承 `v1.1.0` 的 173 张表和最小系统数据，用于快速体验、自动化验收和 issue 复现。 |
| `bpmt` | `database/bpmt.sql.gz` | 使用当前整理后的 `bpmt` 数据库导出的完整初始化库，用于更接近历史业务数据的本地试运行。 |

共存原则：

- MariaDB 同一个实例中允许同时存在 `bpmt` 和 `bpmt_min` 两个 database。
- 两份 SQL 必须各自包含 `CREATE DATABASE IF NOT EXISTS ...`、`USE ...`，避免依赖 compose 中的 `MARIADB_DATABASE` 单库初始化语义。
- Web 应用实际连接哪个库，由 `docker-compose.yml` 中 `DB_NAME` 决定。
- 默认初始化脚本只下载并导入 `bpmt.sql`，参数 `min` 切换为下载并导入 `bpmt-min.sql`。
- 切换运行库时不要求重建 MariaDB 容器，只需要修改 `DB_NAME` 后重启 Web 容器；如果 SQL 未导入过，再执行初始化脚本。

建议脚本形态：

```bash
scripts/init-db.sh
scripts/init-db.sh min
```

验收要求：

- `scripts/init-db.sh` 默认创建或更新 `db/init/bpmt.sql`，可从 `database/bpmt.sql.gz` 自动解压。
- `scripts/init-db.sh min` 创建或更新 `db/init/bpmt-min.sql`。
- 首次 `docker compose up -d` 后，MariaDB 中存在目标数据库。
- 同一次初始化中导入 `bpmt` 和 `bpmt_min` 不互相覆盖。
- `DB_NAME=bpmt docker compose up -d web` 可连接完整库。
- `DB_NAME=bpmt_min docker compose up -d web` 可连接最小库。

## README 重构规划

README 面向初学者重构为以下结构：

1. 项目是什么：一句话解释 BPMT、适用场景和本项目边界。
2. 最快启动：一条命令下载 compose 和默认 `bpmt` 初始化库。
3. 使用最小库启动：说明 `scripts/init-db.sh min` 和 `DB_NAME=bpmt_min`。
4. 登录信息：在启动命令旁边直接写 `admin/admin`。
5. 常见操作：查看状态、停止、重启 Web、重新初始化数据库。
6. 数据库选择：解释 `bpmt` 与 `bpmt_min` 的差异、共存方式和切换方式。
7. 维护者构建：Java 8、Maven、Docker、`settings.local.xml`、镜像构建。
8. 故障排查：数据库未导入、端口占用、旧 `db/data` 未清理、设计器 404 回归检查。
9. 许可证与作者：预告未来使用 MIT，作者 `wodenwang`、`borballzhai`。

README 不再要求普通使用者理解 Maven 或 IDE 配置；这些内容放入维护文档。

## Codex agent 团队开发模式

`AGENTS.md` 在 `v1.2.0` 中作为团队协作入口维护：

- 先读 `AGENTS.md`，再读 README 和 `docs/v1.2.0/*`。
- 版本工作按阶段推进：bug 修复、数据库初始化、文档重构、品牌清理、发布验收。
- 每个阶段要留下可验证结果，不只写说明。
- 涉及数据库、Docker、构建、发布的变更必须同步更新 README 或维护文档。
- 不提交 `.vscode/`、`settings.local.xml`、运行数据、私有 SQL、日志和本地覆盖配置。

## 品牌和 copyright

默认展示信息调整为：

- logo：简约 `BPMT` 字样图片，尺寸覆盖现有入口 logo。
- 页面标题：保留 `BPMT` 或 `BPMT Lite`。
- 默认 copyright：`Copyright (c) 2026 wodenwang and borballzhai`。
- 去掉默认运行页面中的 `Riversoft Designs` 展示字样。

注意：代码包名、Maven 坐标、历史源码路径中仍可能存在 `riversoft`，本版本不做大规模重命名，避免破坏遗留系统。

## 验收基线

发布前至少完成：

```bash
scripts/verify-repo.sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -DskipTests compile
scripts/build-image.sh
docker compose config
```

运行验收至少覆盖：

- 默认 `bpmt` 初始化后 `/` 返回 200。
- 默认 `bpmt` 初始化后 `/ueditor/` 返回 200。
- `bpmt_min` 初始化后 `/` 返回 200。
- `bpmt_min` 初始化后 `/ueditor/` 返回 200。
- 工作流设计入口不再跳转到 404。
- 审批流流程图可显示，节点中文不再显示为方框。
- 本地 Docker 镜像可按当前机器原生架构构建，Apple Silicon 上不再强制 `AMD64`。
- 默认页面 copyright 不包含 `Riversoft Designs`。
