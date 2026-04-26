# bpmt-lite

`bpmt-lite` 是 BPMT 低代码平台的简化发行工程。BPMT 表示 BPM + table，核心能力是自定义工作流和动态表格。

本项目只调整代码结构、打包方式、配置方式和部署方式，不升级技术栈、不重写功能、不增加功能。运行栈继续保持 Java 8、Tomcat 7、MariaDB。

## 使用者运行

默认使用已发布 Docker 镜像 `ghcr.io/wodenwang/bpmt-lite:1.0.0`：

```bash
docker compose up -d
```

如果需要初始化干净数据库，将 `kyq.sql` 放到：

```text
db/init/kyq.sql
```

MariaDB 只会在首次创建 `db/data` 数据目录时自动导入该文件。数据库已经初始化后，替换 `kyq.sql` 不会自动重新导入。

## 运行目录

运行时目录约定如下：

```text
db/init/kyq.sql        首次初始化数据库备份，不提交 git
db/data/               MariaDB 数据目录，不提交 git
db/logs/               MariaDB 日志目录，不提交 git
runtime/attachment/    BPMT 附件目录，不提交 git
runtime/download/      BPMT 下载目录，不提交 git
runtime/ueditor-upload/ UEditor 上传目录，不提交 git
runtime/platform-logs/ BPMT 平台日志目录，不提交 git
runtime/tomcat-logs/   Tomcat 日志目录，不提交 git
config/overrides/      properties 覆盖文件目录，不提交具体覆盖文件
```

`config/overrides/*.properties` 会追加到容器启动时生成的同名 properties 文件后面，因此覆盖文件中的同名 key 优先级更高。

## 维护者构建

维护者需要 Java 8、Maven、Docker，以及可访问旧私有依赖的 Maven 仓库。

```bash
cp settings.example.xml settings.local.xml
scripts/build-image.sh
```

`settings.local.xml` 不提交到 git。

## 项目语言

本项目沟通和文档统一使用简体中文。代码、命令、配置键名、Maven 坐标、镜像名等技术标识保持原样。
