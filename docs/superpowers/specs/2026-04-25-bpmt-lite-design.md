# bpmt-lite 设计文档

## 目标

创建 `bpmt-lite`，作为现有 BPMT 低代码平台的简化版、Docker 优先发行形态。

BPMT 表示 BPM + table。平台核心能力是现有的自定义工作流和动态表格 Web 应用。本次迁移必须保持现有功能行为，继续保留旧技术栈：Java 8、Tomcat 7、Maven、MariaDB。本项目只做代码结构、打包方式、配置方式和部署方式的简化，不做功能重写，也不做技术升级。

## 语言约定

本项目统一使用简体中文沟通和维护文档。

包括但不限于：

- 设计文档
- 实施计划
- README 和部署说明
- 项目内操作手册
- 后续交付总结

代码、配置键名、命令、路径、Maven 坐标、镜像名等技术标识保持原样，不强行翻译。

## 源码基线

源码参考基线是当前 RiverSoft trunk：

`/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk`

以当前 trunk 为参考，但只迁入已经确认必要的 Java 8 构建稳定性改动和 Docker 运行支持改动。不能把 trunk 工作副本中的试验性本地改动整体照搬到 `bpmt-lite`。

本地 `bpmt-lite` 仓库对应的 GitHub 仓库是：

`https://github.com/wodenwang/bpmt-lite.git`

## 范围

本项目范围内：

- 创建干净的 `bpmt-lite` Git 仓库。
- 保留构建和运行平台所需的最小 Maven 多模块结构。
- 保留 `platform` 作为核心 Web 应用。
- 构建基于 Tomcat 7 和 Java 8 的 Docker 镜像。
- 将 `platform` 部署为 Tomcat `ROOT`。
- 将富文本编辑器作为独立 `/ueditor` Web 应用部署。
- 提供面向使用者的 `docker-compose.yml`，运行一个 Web 容器和一个 MariaDB 容器。
- 支持通过本地 `db/init/kyq.sql` 初始化数据库，该文件不提交到 git。
- 支持通过 docker compose 配置旧系统的 `*.properties` 参数。
- 将数据库数据、数据库日志、附件、下载文件、平台日志、Tomcat 日志持久化到宿主机。
- 记录维护者构建时对私有或老旧 Maven 依赖的要求。

本项目范围外：

- 升级 Java、Tomcat、Spring、Hibernate、Activiti、MariaDB 驱动或应用框架。
- 改造成 Spring Boot。
- 修改 BPMT 产品功能。
- 增加新产品功能。
- 将 `kyq.sql` 提交到 git。
- 将 Aspose、JPedal、`ueditor.war`、`patch-implementation` 或其他私有历史二进制依赖提交到 git。
- 迁移旧的离线 `package`、`tools`、`support` 发行体系。

## 仓库结构

新仓库只保留最小源码模块：

```text
bpmt-lite/
  pom.xml
  parent/
  util/
  magic/
    magic-api/
    magic-api-impl/
  dbtools/
  platform/
  docker/
  deploy/
  db/
    init/
  runtime/
  docs/
```

具体 Docker 文件目录可以在实施阶段调整，但设计意图是：

- 源码模块保持与原项目可识别的对应关系。
- 运行和部署文件与 Java 源码模块分离。
- 不迁移旧的 `package`、`tools`、`support` 模块。
- 构建产物、本地运行数据、密钥、`kyq.sql` 和大型二进制依赖都必须被 git 忽略。

## Maven 模块

保留这些模块：

- `parent`：依赖和插件管理。
- `util`：RiverSoft 公共工具。
- `magic/magic-api`：license/magic API 契约。
- `magic/magic-api-impl`：`platform` 运行时需要的实现。
- `dbtools`：`platform` 使用的数据库工具依赖。
- `platform`：主 WAR。

不迁入这些模块：

- `package`：旧的离线 zip 发行工程。
- `tools`：旧运维工具包。
- `support`：archetype、generator、hbm2ddl、lightly build 等辅助模块。

这样既保留原有模块边界，又去掉旧发行体系。

## 构建模型

维护者构建仍然基于 Maven 和 Java 8。仓库提交 `settings.example.xml`，维护者在需要本机 Maven 仓库路径或凭据时复制为被 git 忽略的 `settings.local.xml`。典型构建命令是：

```bash
mvn -s settings.local.xml -pl platform -am -Pdocker-image verify
```

项目可以额外提供 `make image` 或 `scripts/build-image.sh` 之类的便捷封装，但底层仍然调用 Maven 构建，不能引入新的构建体系。

Docker 镜像构建流程：

1. 构建 `platform.war`。
2. 从 Maven 解析 `ueditor.war`。
3. 构建 Tomcat 7 + Java 8 镜像。
4. 将 `platform.war` 解压到 `/usr/local/tomcat/webapps/ROOT`。
5. 将 `ueditor.war` 解压到 `/usr/local/tomcat/webapps/ueditor`。

## 历史 Maven 依赖

部分旧依赖预计无法从公共 Maven 仓库获取，包括 Aspose、JPedal、`ueditor.war`、`patch-implementation`。

仓库不提交这些二进制文件。处理方式是：

- 在 POM 中保留 Maven 坐标。
- 提供 `settings.example.xml`。
- 文档说明维护者需要历史私有 Maven 仓库，或包含这些 artifact 的本机 Maven 仓库。
- 将长期依赖归档和供应链治理作为后续独立项目处理。

最终使用者不应该需要 Maven 或这些私有依赖，只消费已发布的 Docker 镜像。

## 发布运行形态

面向使用者的部署默认使用已发布镜像：

```yaml
image: ghcr.io/wodenwang/bpmt-lite:<version>
```

普通使用路径：

1. 创建一个部署目录。
2. 放入 `docker-compose.yml`。
3. 可选放入初始化数据库备份 `db/init/kyq.sql`。
4. 执行 `docker compose up -d`。

默认运行两个服务：

- `web`：Tomcat 7 + Java 8 + `ROOT` + `/ueditor`。
- `mariadb`：数据库名为 `kyq` 的 MariaDB 实例。

## 数据库

运行数据库使用 MariaDB。默认数据库名是 `kyq`。

compose 暴露简化数据库配置：

```yaml
DB_HOST: mariadb
DB_PORT: 3306
DB_NAME: kyq
DB_USER: root
DB_PASSWORD: 123456
```

默认 compose 不暴露原始 JDBC URL。容器入口根据这些简化数据库配置生成 JDBC URL：

```properties
jdbc.driverClassName=com.mysql.jdbc.Driver
jdbc.url=jdbc:mysql://mariadb:3306/kyq?useUnicode=true&characterEncoding=UTF-8
jdbc.username=root
jdbc.password=123456
hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
```

入口脚本同时生成匹配的 `db.properties`，包括 `db.def.*` 配置。

数据库初始化：

- `db/init/kyq.sql` 是初始化备份的约定本地路径。
- `kyq.sql` 不提交到 git。
- MariaDB 首次初始化时通过 `/docker-entrypoint-initdb.d` 自动导入。
- 文档必须说明：MariaDB 数据目录已存在后，再修改 `kyq.sql` 不会自动重新导入。

MariaDB 配置应适配较大的历史备份，包括 UTF-8 默认字符集、`lower_case_table_names=1`、较大的 packet size、较长读写超时时间。

## 运行卷映射

所有需要在容器替换后保留的状态都映射到宿主机：

```yaml
web:
  volumes:
    - ./runtime/attachment:/usr/local/tomcat/webapps/attachment
    - ./runtime/download:/usr/local/tomcat/webapps/download
    - ./runtime/platform-logs:/usr/local/tomcat/webapps/logs
    - ./runtime/tomcat-logs:/usr/local/tomcat/logs

mariadb:
  volumes:
    - ./db/data:/var/lib/mysql
    - ./db/init:/docker-entrypoint-initdb.d
    - ./db/logs:/var/log/mysql
```

附件和下载目录刻意沿用原 package 布局。旧运行形态中，`platform`、`ueditor`、`attachment` 等目录都在 package 部署根目录下。容器中由 `/usr/local/tomcat/webapps` 承担同样角色：

- `/usr/local/tomcat/webapps/ROOT`：原 `platform`。
- `/usr/local/tomcat/webapps/ueditor`：富文本编辑器 Web 应用。
- `/usr/local/tomcat/webapps/attachment`：默认附件目录。
- `/usr/local/tomcat/webapps/download`：默认下载目录。
- `/usr/local/tomcat/webapps/logs`：BPMT 平台日志。

默认不暴露 `file.attachment.path`。让应用继续使用原有的默认平台根目录逻辑。只有后续验证证明必须覆盖时，才增加该参数。

MariaDB 日志配置必须保证 `./db/logs` 能收到有效数据库日志，而不是一个无实际用途的空挂载。

## Properties 配置

compose 文件是部署配置入口。使用者不需要进入 Docker 镜像或修改 WAR 文件来配置应用。

旧应用从 `WEB-INF/classes` 读取 `*.properties`。Docker entrypoint 在容器启动时根据默认值和环境变量生成同名 properties 文件，用这种方式兼容旧代码。

需要生成的 properties 文件包括：

- `jdbc.properties`
- `db.properties`
- `page.properties`
- `safe.properties`
- `sms.properties`
- `wx.properties`
- `mail.properties`
- `office.properties`
- `log.properties`
- `hazelcast.properties`
- `activiti.properties`
- `redis.properties`
- `quartz.properties`

默认值尽量来自原 `package/src/main/package/conf/*.properties`，但有这些有意调整：

- 数据库默认改为 MariaDB `kyq`，不再使用 H2。
- 敏感字段默认使用空值或无害示例值。
- 外部集成能力尽量默认关闭。

环境变量命名使用机械规则：

- properties key 转成大写。
- 点号替换为下划线。

示例：

```text
page.title -> PAGE_TITLE
page.frame.new -> PAGE_FRAME_NEW
page.browser.msg -> PAGE_BROWSER_MSG
safe.sync.threads -> SAFE_SYNC_THREADS
sms.ali.enable -> SMS_ALI_ENABLE
wx.web.login.qrcode -> WX_WEB_LOGIN_QRCODE
mail.flow.subject.type -> MAIL_FLOW_SUBJECT_TYPE
office.flag -> OFFICE_FLAG
log.keepdays -> LOG_KEEPDAYS
hazelcast.group.name -> HAZELCAST_GROUP_NAME
activiti.font -> ACTIVITI_FONT
```

面向使用者的 compose 文件应列出常用和重要变量。`page.properties` 需要尽量完整呈现，因为这些值通常属于部署侧的品牌、页面和 UI 行为配置。

长文本配置，例如邮件通知模板，必须支持两种方式：

- 简单场景直接通过 compose 环境变量设置。
- 对于不适合写进 YAML 的长文本，支持挂载 `./config/overrides/*.properties` 覆盖文件。

当覆盖文件存在时，它应优先于同名 properties 文件的生成默认值。默认 compose 要保持可读，同时展示覆盖文件的挂载方式。

## Docker 镜像边界

镜像可以包含：

- Tomcat 7 + Java 8 运行时。
- 解压后的 `ROOT` Web 应用。
- 解压后的 `/ueditor` Web 应用。
- 通用 entrypoint 脚本。
- 默认 properties 模板。

镜像不能包含环境特定配置、数据库备份、本地密钥或宿主机特定路径。

启动时 entrypoint 执行：

1. 确保所需运行目录存在。
2. 根据默认值和环境变量生成 `WEB-INF/classes/*.properties`。
3. 使用 `catalina.sh run` 启动 Tomcat。

## Docker Compose 边界

默认 compose 文件面向最终使用者，引用已发布镜像，不构建 Java 源码。

仓库可以包含单独的维护者构建 compose 或脚本，但命名必须清晰，避免使用者混淆构建流程和运行流程。

默认暴露 HTTP 端口可配置：

```yaml
BPMT_HTTP_PORT: 8080
```

`web` 服务必须等待 MariaDB 健康检查通过后再启动。

## Git 和仓库卫生

本地仓库使用 `main` 分支，远端为 `https://github.com/wodenwang/bpmt-lite.git`。

不要提交：

- `kyq.sql`
- `db/data`
- `db/logs`
- `runtime`
- Maven `target`
- `.svn`
- 私有二进制依赖
- `settings.local.xml` 或任何带密钥的本地 settings 文件
- cookies 或临时浏览器文件

第一轮实施应包含 `.gitignore` 和清晰的 README 指引。

## 验证

验证目标是证明迁移保留了原行为，同时简化了运行方式。

仓库验证：

- 确认只包含最小模块和 Docker/文档文件。
- 确认没有提交旧 `package`、`tools`、`support`、`.svn`、`target`、`kyq.sql`、密钥或私有二进制文件。
- 确认 git remote 指向 `https://github.com/wodenwang/bpmt-lite.git`。

构建验证：

- 使用 Java 8 构建。
- 使用文档化的 Maven settings。
- 生成 `platform.war`。
- 构建 Docker 镜像。
- 确认镜像同时包含 `ROOT` 和 `ueditor`。

容器验证：

- 使用 compose 启动 MariaDB 和 web。
- 验证 MariaDB 健康检查。
- 验证首次启动时可选 `db/init/kyq.sql` 导入行为。
- 验证生成的 properties 文件存在，并反映 compose 配置。
- 验证 Tomcat 启动并部署两个应用。

行为验证：

- 访问 `http://127.0.0.1:<port>/`。
- 访问原平台使用的主登录页或首页路径。
- 访问 `/ueditor/`。
- 如果初始化数据和账号允许，验证一个最小上传路径。
- 确认附件文件持久化到 `./runtime/attachment`。
- 确认下载文件持久化到 `./runtime/download`。
- 确认 BPMT 平台日志持久化到 `./runtime/platform-logs`。
- 确认 Tomcat 日志持久化到 `./runtime/tomcat-logs`。
- 确认 MariaDB 日志持久化到 `./db/logs`。

任何验证步骤都不能增加新的产品行为。

## 后续事项

Aspose、JPedal、`ueditor.war`、`patch-implementation` 等旧私有依赖的长期处理方式暂不纳入本次迁移。当前项目只文档化所需 Maven 访问路径，并保持这些二进制文件不进入 git。后续可单独设计稳定的依赖归档和授权治理方案。
