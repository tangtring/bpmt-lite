# v1.1.0 极简 kyq 初始化库 spec

## 背景

`v1.0.0` 支持通过 `db/init/kyq.sql` 初始化完整业务数据库，但公开仓库不包含可分发 SQL。使用者如果没有历史 `kyq.sql`，只能启动空数据库，无法得到可体验的 BPMT 初始化状态。

`v1.1.0` 需要清洗一份更小、更适合公开分发的 `kyq` 初始化库，让使用者可以用 Docker compose 快速得到一个最小可访问环境。

## 目标

- 交付一份可公开提交或作为 Release asset 发布的极简 `kyq` SQL。
- 初始化后平台首页 `/` 返回 200，`/ueditor/` 返回 200。
- 初始化库只包含系统启动、登录、基础权限、基础菜单和必要元数据。
- 清洗掉历史业务数据、客户数据、流程实例数据、附件引用、消息记录、日志和第三方集成账号。
- 形成可重复的清洗脚本或操作说明，避免手工不可追溯。

## 非目标

- 不迁移或改造表结构。
- 不把完整历史业务库公开分发。
- 不修复历史测试数据质量问题。
- 不保证所有业务流程、报表、动态表单样例都可用。
- 不在本 spec 中调整 Docker compose 配置结构。

## 清洗原则

极简库按“保留系统骨架，删除业务内容”的原则处理。

优先保留：

- 系统用户、角色、权限、菜单和基础字典。
- 平台启动必须读取的配置表。
- 开发平台元数据中与首页、菜单、基础页面访问直接相关的记录。
- Hibernate 或调度器启动所需的空表结构。

优先删除或置空：

- 流程实例、任务实例、历史审批意见和运行中队列。
- 动态业务表中的业务数据。
- 报表结果、统计缓存、SQL 执行历史和访问日志。
- 邮件、短信、微信、企业微信、支付和第三方回调历史。
- 附件、下载文件、UEditor 上传文件的历史引用。
- 真实手机号、邮箱、微信标识、客户名称、账号密钥等敏感数据。

## 交付物

推荐交付结构：

```text
db/init/minimal-kyq.sql
scripts/export-minimal-kyq.sh
docs/v1.1.0/spec-minimal-kyq.md
```

如果 SQL 体积过大，不直接提交到 git，则改为 GitHub Release asset：

```text
bpmt-lite-minimal-kyq-1.1.0.sql.gz
```

仓库中仍需保留清洗脚本、校验脚本和 SHA256。

## 验收标准

使用全新运行目录验证：

1. 删除或换用全新的 `db/data`。
2. 将极简 SQL 放入 `db/init/kyq.sql`。
3. 执行 `docker compose up -d`。
4. MariaDB 健康检查通过。
5. 表数量记录到发布文档中。
6. `/` 返回 200。
7. `/ueditor/` 返回 200。
8. 若包含默认登录账号，必须记录账号来源和初始化方式；若不包含默认账号，README 必须明确说明。

验收命令示例：

```bash
docker compose down
rm -rf db/data
cp db/init/minimal-kyq.sql db/init/kyq.sql
docker compose up -d
docker compose ps
docker compose exec -T mariadb mariadb -uroot -p123456 -N \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='kyq';"
curl -fsSI http://127.0.0.1:8080/
curl -fsSI http://127.0.0.1:8080/ueditor/
```

## 风险和决策点

- 是否保留默认管理员账号需要单独确认。保留账号体验更好，但必须避免默认弱密码在生产环境被误用。
- 如果清洗后首页依赖某些历史菜单或元数据，需要把这些记录纳入“系统骨架”而不是业务数据。
- 如果 SQL 体积仍明显偏大，应优先继续删数据，而不是压缩后直接发布。
- 如果公开 SQL 涉及任何历史客户或人员痕迹，不能发布。

## 实施顺序

1. 从当前可运行 `kyq` 库导出结构和数据概览。
2. 按表名前缀和 Hibernate 映射归类系统表、运行表、业务表、日志表。
3. 先生成只含结构的 SQL。
4. 补入最小系统数据。
5. 使用 Docker compose 从零导入验证。
6. 根据启动日志和页面访问结果补齐必要元数据。
7. 固化清洗脚本和验收记录。
