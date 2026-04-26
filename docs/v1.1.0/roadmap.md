# bpmt-lite v1.1.0 迭代总纲

`v1.1.0` 的目标是继续收口 bpmt-lite 的发行体验，让维护者更容易构建，让使用者更容易完成一次可用的本地初始化。

本版本仍遵守项目边界：不升级 Java、Tomcat、Spring、MariaDB 等技术栈，不重写业务功能，不增加新的业务能力。

## 版本目标

本版本拆成三个独立 spec 推进：

1. 历史 Maven 依赖分发
   - 解决公共 Maven 仓库无法下载部分旧 jar 导致新环境无法编译的问题。
   - 当前结论：通过依赖裁剪和坐标调整，已不再需要额外分发历史 Maven artifact。
2. 极简 `kyq` 初始化库
   - 清洗一份更小、更适合公开分发的初始化数据库。
   - 交付可导入 SQL、数据清洗说明和 Docker compose 初始化验收基线。
3. `docker-compose.yml` 配置瘦身
   - 默认 compose 只暴露必要配置。
   - 绝大部分旧 properties 参数保留在容器默认值或 `config/overrides` 机制中。

## 非目标

- 不替换老旧 jar 的实现。
- 已明确裁剪的历史功能不再进入 v1.1.0 默认发行。
- 不将项目迁移到新的构建系统。
- 不修改 BPMT 业务功能。
- 不修改数据库表结构来适配新功能。
- 不改变 v1.0.0 已确认的默认访问入口：`http://127.0.0.1:8080/` 和 `/ueditor/`。
- 不取消 `config/overrides/*.properties` 覆盖机制。

## 推进顺序

1. 先推进历史 Maven 依赖分发。
   - 这是维护者构建链路的前置问题。
   - 验收结果应证明新机器只依赖公共 Maven 仓库即可完成编译。
   - 在进入实现前，先完成 `pretask-office-dependency-assessment.md`，确认 Aspose、JPedal、JODConverter 三组依赖的保留或割舍策略。
2. 再推进极简 `kyq` 初始化库。
   - 这是使用者快速体验的核心问题。
   - 验收结果应证明公开 SQL 可以初始化出能访问核心页面的数据库。
3. 最后推进 compose 配置瘦身。
   - 这一步应基于前两项形成新的 Quick Start。
   - 验收结果应证明默认 compose 更短、更可读，同时高级配置仍可通过 override 处理。

## 发布验收基线

`v1.1.0` 发布前至少完成以下验证：

- `scripts/verify-repo.sh` 通过。
- Java 8 下 Maven 全仓编译通过。
- 使用空 Maven 本地仓库和公共 Maven 设置时，全仓编译通过。
- `scripts/build-image.sh` 构建 `ghcr.io/wodenwang/bpmt-lite:1.1.0` 通过。
- 使用极简 `kyq` SQL 初始化后，MariaDB 健康检查通过。
- 使用极简 `kyq` SQL 初始化后，`/` 返回 200。
- 使用极简 `kyq` SQL 初始化后，`/ueditor/` 返回 200。
- 默认 `docker-compose.yml` 中只保留必要环境变量。
- 至少验证一个 `config/overrides/*.properties` 覆盖文件仍然生效。

## 当前状态

截至 2026-04-26：

- 历史 Maven 依赖阻断已通过依赖裁剪和公开坐标替换解决，不再需要额外分发历史 Maven artifact。
- `database/bpmt-db.sql` 已生成并作为最小初始化库，导入后共 173 张表。
- `docker-compose.yml` 已完成配置瘦身，高级配置通过 `config/overrides/*.properties` 覆盖。
- 本地发布候选镜像 `ghcr.io/wodenwang/bpmt-lite:1.1.0` 已构建并完成 compose 启动验收。

## 文档结构

本版本文档放在：

```text
docs/v1.1.0/
  roadmap.md
  pretask-office-dependency-assessment.md
  spec-historical-dependencies.md
  spec-minimal-kyq.md
  spec-compose-config.md
```

每个 spec 独立进入实现计划、实现、验收和发布记录。
