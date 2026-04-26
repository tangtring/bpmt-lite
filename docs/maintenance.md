# bpmt-lite 维护说明

本文面向维护者，记录本项目的构建、验证、发布和常见处理方式。

## 基本原则

- 项目沟通和文档使用简体中文
- 保留 Java 8、Tomcat 7、MariaDB
- 不为工程整理主动升级底层技术栈
- 不做功能扩展，除非有明确版本目标
- 不提交本地运行数据、数据库备份、私有依赖和许可证文件

## 本地前置条件

维护构建需要：

- Java 8
- Maven
- Docker
- 可访问旧私有依赖的 Maven 配置
- GitHub/GHCR 发布权限

确认 Java 版本：

```bash
java -version
```

如果机器上有多个 JDK，构建时显式指定 Java 8：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home/bin:$PATH \
mvn -s settings.local.xml -pl platform -am -Pdocker-image -DskipTests compile
```

## Maven 配置

首次构建前复制本地配置：

```bash
cp settings.example.xml settings.local.xml
```

`settings.local.xml` 不提交到 git。它用于配置可访问旧私有依赖的 Maven 仓库。

当前仍可能依赖旧包，例如 patch implementation、UEditor WAR 等。不要在没有专项计划的情况下替换这些依赖。

## 仓库检查

提交前运行：

```bash
scripts/verify-repo.sh
```

该脚本会检查：

- 必要模块存在
- 旧发行相关模块没有迁入
- 本地运行数据没有进入 git
- 私有依赖、许可证、数据库备份没有进入 git
- origin 指向 `https://github.com/wodenwang/bpmt-lite.git`

## 构建镜像

使用：

```bash
scripts/build-image.sh
```

脚本会：

- 检查 `settings.local.xml`
- 检查当前 Java 是否为 Java 8
- 执行 Maven `-Pdocker-image verify`
- 构建 `ghcr.io/wodenwang/bpmt-lite:<project.version>`

## 本地运行验证

准备数据库初始化文件：

```text
db/init/kyq.sql
```

启动：

```bash
docker compose up -d
```

检查状态：

```bash
docker compose ps
```

检查数据库表数量：

```bash
docker compose exec -T mariadb mariadb -uroot -p123456 -N \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='kyq';"
```

`v1.0.0` 的期望结果是 `383`。

检查 Web：

```bash
curl -fsSI http://127.0.0.1:8080/
curl -fsSI http://127.0.0.1:8080/ueditor/
```

期望均返回 `HTTP/1.1 200 OK`。

## 发布流程

正式发布建议按以下顺序：

1. 新建发布分支
2. 修改 Maven 版本号
3. 修改 `docker-compose.yml` 默认 `BPMT_IMAGE_TAG`
4. 更新 README 和 release 文档
5. 运行 `scripts/verify-repo.sh`
6. 使用 Java 8 编译
7. 执行 `scripts/build-image.sh`
8. 推送 GHCR 镜像
9. 匿名拉取镜像验证
10. 基于 tag 或干净克隆运行 compose 验证
11. 合并 PR 到 `main`
12. 在合并后的 `main` 打 git tag
13. 创建 GitHub Release

## GHCR 发布

登录 GHCR：

```bash
gh auth token | docker login ghcr.io -u wodenwang --password-stdin
```

推送镜像：

```bash
docker push ghcr.io/wodenwang/bpmt-lite:<tag>
```

匿名拉取验证：

```bash
TMP_DOCKER_CONFIG=$(mktemp -d)
DOCKER_CONFIG="$TMP_DOCKER_CONFIG" docker pull --platform linux/amd64 ghcr.io/wodenwang/bpmt-lite:<tag>
rm -rf "$TMP_DOCKER_CONFIG"
```

查看 digest：

```bash
crane digest ghcr.io/wodenwang/bpmt-lite:<tag>
```

本机曾出现 Docker push 到 GHCR 大层断流。如果新构建镜像与已验证镜像运行内容一致，可以使用 registry 级别重标记作为发布补救方案：

```bash
crane copy ghcr.io/wodenwang/bpmt-lite:<source-tag> ghcr.io/wodenwang/bpmt-lite:<target-tag>
```

`v1.0.0` 发布时，`1.0.0` 和 `latest` 都重标记到已验证 digest：

```text
sha256:8d3071c2b43e472beb0f453990b95c057895bf02bdd4be0dcdf74e7b336ba961
```

使用该方案前必须确认运行内容没有实质差异，并完成 compose 验收。

## 数据和目录

不要提交以下内容：

- `db/init/*.sql`
- `db/data/`
- `db/logs/`
- `runtime/`
- `config/overrides/*.properties`
- `settings.local.xml`
- 私有依赖 jar、war
- 许可证文件

`db/init/kyq.sql` 只用于首次初始化。已有 `db/data` 时不会再次导入。

## v1.1.0 Office/PDF 转换裁剪

`v1.1.0` 默认发行不再包含 Aspose、JPedal 和 JODConverter。

影响范围：

- 不支持微信/企业微信文件自动转图文素材。
- 不支持非 PDF Office 附件在线转 PDF 预览。
- 不支持依赖 Office 服务的 PDF 导出。
- PDF 文件本身的普通上传、下载和直接预览不受影响。

默认 Docker 配置继续保持 `office.flag=false` 和 `office.prepare=false`。后续如果要恢复 Office/PDF 转换能力，应作为单独版本目标重新设计依赖来源、许可边界和运行服务。

## 常见问题

### 修改 kyq.sql 后为什么没有重新导入？

MariaDB 官方初始化机制只在数据目录为空时执行 `/docker-entrypoint-initdb.d`。需要重新导入时，先备份数据，再删除 `db/data` 后重启。

### 为什么不升级 Java、Tomcat、Spring 或 MariaDB？

本项目目标是发行方式现代化，不是技术栈升级。老技术栈虽然陈旧，但业务功能已经稳定；贸然升级会带来较高回归风险。

### 为什么 docker-compose.yml 只保留少量配置？

`v1.1.0` 起，默认 compose 只保留快速启动需要的端口、镜像 tag 和数据库连接信息。原 properties 的低频参数仍由 `docker/docker-entrypoint.sh` 生成默认值。

需要调整高级配置时，在 `config/overrides/` 下创建同名 properties 文件，例如：

```text
config/overrides/page.properties
config/overrides/log.properties
config/overrides/jdbc.properties
```

覆盖文件会追加到容器内 `ROOT/WEB-INF/classes/` 的同名文件末尾，因此同名 key 以覆盖文件为准。仓库中的 `*.example` 文件只作为模板，不会被运行时读取。

### UEditor 上传文件保存在哪里？

容器内路径：

```text
/usr/local/tomcat/webapps/ueditor/upload
```

宿主机映射路径：

```text
runtime/ueditor-upload
```
