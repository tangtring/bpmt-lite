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
- multi-arch 发布脚本的静态检查通过

## 构建镜像

本地 Web 镜像构建使用：

```bash
scripts/build-image.sh
```

脚本会：

- 检查 `settings.local.xml`
- 检查当前 Java 是否为 Java 8
- 执行 Maven `-Pdocker-image verify`
- 构建 `ghcr.io/wodenwang/bpmt-lite:<project.version>`
- 启动一次临时容器，验证 `ROOT`、`ueditor`、entrypoint 和 CJK 字体可用

该入口用于本机 smoke，默认构建当前 Docker daemon 对应架构，不作为正式发布推送入口。

本地 Web 镜像构建默认使用 `https://mirrors.aliyun.com/ubuntu` 作为 amd64 Ubuntu 源；arm64/ports 架构由 Dockerfile 默认 `APT_PORTS_MIRROR=https://mirrors.aliyun.com/ubuntu-ports` 处理。需要使用其他 Ubuntu 源时可覆盖 Maven 属性：

```bash
mvn -s settings.local.xml -pl platform -am -Pdocker-image verify -Ddocker.apt.mirror=http://archive.ubuntu.com/ubuntu
```

## API 镜像构建

`v1.4.1` 起使用独立 API 镜像，构建入口为：

```bash
scripts/build-api-image.sh
```

脚本会：

- 检查 `settings.local.xml`
- 检查当前 Java 是否为 Java 8
- 执行 Maven `-pl api -am -Pdocker-image package`
- 构建 `ghcr.io/wodenwang/bpmt-lite-api:<project.version>`
- 启动一次临时容器，验证 `/usr/local/tomcat/webapps/api`、`openapi.json`、`docs/index.html` 和 entrypoint 可用

API 镜像复用 `docker/docker-entrypoint.sh` 生成数据库、Hibernate、Hazelcast 等 properties；容器内 `APP_CLASSES` 指向 `/usr/local/tomcat/webapps/api/WEB-INF/classes`。

该入口用于本机 smoke，默认构建当前 Docker daemon 对应架构，不作为正式发布推送入口。

## Multi-arch 发布镜像

`v1.5.4` 起，正式发布到 GHCR 的 Web/API 镜像必须同时包含 `linux/amd64` 和 `linux/arm64`。发布入口为：

```bash
scripts/build-multiarch-images.sh
```

脚本会：

- 检查 `settings.local.xml`
- 检查当前 Java 是否为 Java 8
- 使用 Maven 生成 Web/API WAR
- 使用 `docker buildx build --platform linux/amd64,linux/arm64 --push` 推送 Web 镜像
- 使用 `docker buildx build --platform linux/amd64,linux/arm64 --push` 推送 API 镜像
- 默认同步 `ghcr.io/wodenwang/bpmt-lite:latest` 和 `ghcr.io/wodenwang/bpmt-lite-api:latest`
- 使用 `docker buildx imagetools inspect` 检查发布后的 manifest

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BPMT_IMAGE_PLATFORMS` | `linux/amd64,linux/arm64` | 发布平台列表 |
| `BPMT_IMAGE_NAME` | `ghcr.io/wodenwang/bpmt-lite` | Web 镜像名 |
| `BPMT_API_IMAGE_NAME` | `ghcr.io/wodenwang/bpmt-lite-api` | API 镜像名 |
| `BPMT_IMAGE_TAG` | Maven `project.version` | Web 镜像版本 tag |
| `BPMT_API_IMAGE_TAG` | Maven `project.version` | API 镜像版本 tag |
| `BPMT_SYNC_LATEST` | `true` | 是否同步 `latest` |
| `BPMT_DOCKER_APT_MIRROR` | `https://mirrors.aliyun.com/ubuntu` | Docker 构建时的 Ubuntu amd64 镜像 |
| `BPMT_DOCKER_APT_PORTS_MIRROR` | `https://mirrors.aliyun.com/ubuntu-ports` | Docker 构建时的 Ubuntu ports 镜像 |

发布前准备 buildx builder：

```bash
docker login ghcr.io
docker buildx create --name bpmt-multi --use || docker buildx use bpmt-multi
docker buildx inspect --bootstrap
```

如果是候选验证或临时 tag，不希望覆盖 `latest`，可执行：

```bash
BPMT_SYNC_LATEST=false scripts/build-multiarch-images.sh
```

发布后必须确认两个镜像都包含 `linux/amd64` 和 `linux/arm64`：

```bash
docker buildx imagetools inspect ghcr.io/wodenwang/bpmt-lite:<version>
docker buildx imagetools inspect ghcr.io/wodenwang/bpmt-lite-api:<version>
```

后续发布不能只用 Apple Silicon 本机的 `docker build` 结果直接推送正式 tag；否则 x86_64 Linux 服务器会无法拉取匹配架构的 Web/API 镜像。

## 本地运行验证

准备数据库初始化文件：

```text
db/init/bpmt.sql
db/init/bpmt-min.sql
```

启动：

```bash
docker compose up -d
```

检查状态：

```bash
docker compose ps
```

检查最小数据库表数量：

```bash
docker compose exec -T bpmt-mariadb mariadb -uroot -p123456 -N \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='bpmt_min';"
```

`v1.6.0` 最小初始化库 `database/bpmt-min.sql.gz` 的期望结果是 `176`，其中 Activiti 24 张、Quartz 11 张、OAuth 登录表 3 张。完整库 `database/bpmt.sql.gz` 的期望结果是 `380` 张表或视图，默认 `admin` 密码为 `admin`。

`v1.6.0` 运行验收除常规入口、API、OAuth smoke、HTTPS smoke 外，必须继承 `v1.5.1` issue #10 回归基线：使用完整库 `bpmt` 浏览器实点 `/flow/CommonFlowAction/taskList.shtml` 的“查看/处理”，确认网络请求中不再出现 `_ORD_ID=null`。

`v1.6.0` 还需要补充 OAuth 登录态切换验收：已有 BPMT 登录态访问 `/oauth/authorize` 时应复用当前用户；当前用户无目标第三方系统权限时应显示 BPMT 内部提示页；用户取消授权时应返回第三方并携带 `access_denied`；用户选择切换账号时，应退出当前 BPMT 登录态，重新登录后回到原 OAuth 授权流程。

`v1.6.0` 必须在非标准 HTTP/HTTPS 端口上验证 OAuth 主流程，例如 `BPMT_HTTP_PORT=18080`、`BPMT_HTTPS_PORT=18443`。`docker/nginx/nginx.conf` 应使用 `proxy_set_header Host $http_host;` 和公开 `X-Forwarded-*` 头，确保 BPMT 生成的登录页、OAuth 授权页和第三方回调地址保留实际端口和公开 scheme。

检查 Web：

```bash
curl -fsSI http://127.0.0.1/
curl -fsSI http://127.0.0.1/ueditor/
```

期望均返回 `HTTP/1.1 200 OK`。

检查 API 文档：

```bash
curl -fsSI http://127.0.0.1/api/openapi.json
curl -fsSI http://127.0.0.1/api/docs/
```

检查 API 鉴权和动态表列表签名链路：

```bash
scripts/smoke-api.sh
```

### HTTPS 验收

v1.6.0 起，正式发布前必须验证内置 nginx HTTPS 入口。基础 `docker-compose.yml` 默认只发布 HTTP；启用 HTTPS 时必须同时加载 `docker-compose.https.yml`：

```bash
BPMT_HTTPS_ENABLED=1 BPMT_HTTPS_PORT=18443 BPMT_HTTP_PORT=18080 \
  docker compose -f docker-compose.yml -f docker-compose.https.yml up -d
curl -k -fsSI https://127.0.0.1:18443/
curl -k -fsSI https://127.0.0.1:18443/ueditor/
curl -k -fsSI https://127.0.0.1:18443/api/docs/
curl -k -fsSI https://127.0.0.1:18443/api/openapi.json
curl -fsSI http://127.0.0.1:18080/
BPMT_API_BASE_URL=https://127.0.0.1:18443/api BPMT_API_CURL_INSECURE=1 scripts/smoke-api.sh
```

`curl -fsSI http://127.0.0.1:18080/` 期望返回 `301` 并跳转到 HTTPS。若测试 `BPMT_HTTP_REDIRECT=false`，HTTP 与 HTTPS 应同时代理业务。

后端信任 `X-Forwarded-*` 头。生产部署不得把 `bpmt-web` 或 `bpmt-api` 直接暴露到不可信网络；上游网关必须覆盖并规范设置 `X-Forwarded-Proto`、`X-Forwarded-Host` 和 `X-Forwarded-Port`。

API 业务接口默认使用以下环境变量：

```text
BPMT_API_APP_KEY=bpmt-api
BPMT_API_APP_SECRET=bpmt-api-secret
BPMT_API_ACT_AS=admin
```

`BPMT_API_ACT_AS` 未配置或对应用户不可用时兜底 `admin`。`appKey` 和 `appSecret` 必须配置；默认 compose 已给出开发默认值，正式部署应覆盖。

检查 OAuth 主流程：

```bash
curl -fsSI 'http://127.0.0.1/oauth/authorize?response_type=code&client_id=demo-client&redirect_uri=http%3A%2F%2F127.0.0.1%2Fdemo%2Fcallback&state=abc'
```

`v1.5.0` OAuth 主流程完全在 `bpmt-web/platform`，不改 `bpmt-api`。未登录访问 authorize 时应进入 BPMT 登录页，登录成功后回到原始 authorize 请求。`/oauth/token` 和 `/oauth/userinfo` 使用 OAuth JSON 响应，不使用 `success/data/error` 包装。

检查 OAuth INFO 日志：

```bash
find runtime/platform-logs -type f | sort
rg -n "oauth|OAuth|requestId|clientId|thirdpartKey|access_denied|invalid_grant" runtime/platform-logs runtime/tomcat-logs
```

OAuth 日志应能串联 authorize、token、userinfo 的开始和结果，并包含 `clientId`、`thirdpartKey`、`userid`、权限校验结果、错误码和失败原因。日志中禁止出现明文 `code`、`access_token`、`client_secret`、`password`；如需排障，只记录 hash 前缀、记录主键或 requestId。

外部系统 `clientSecret` 重置：

1. 进入 BPMT 后台外部系统管理入口。
2. 找到目标 `CM_THIRDPART` 记录。
3. 进入编辑页，在“重置密钥”输入框手工填写新 secret。
4. 保存后系统只写入新的 `CLIENT_SECRET_HASH`，不会回显新 secret。
5. 将同一新 secret 更新到第三方系统服务端配置。
6. 使用新 secret 完成一次 authorize -> token -> userinfo 验收。
7. 确认旧 secret 已无法换取 token。

注意：新增外部系统时，系统会生成 `clientSecret` 并只展示一次；编辑页重置密钥不是系统生成展示流程，而是维护者手工输入新 secret 后保存。

OAuth 状态表排障：

| 表 | 重点字段 | 用途 |
| --- | --- | --- |
| `CM_THIRDPART` | `CLIENT_ID`、`CLIENT_SECRET_HASH`、`REDIRECT_URIS`、`PRI_KEY`、`ACTIVE_FLAG` | 外部系统启停、回调白名单、权限点和 client 凭证 |
| `CM_THIRDPART_AUTH_CODE` | `CODE_HASH`、`CLIENT_ID`、`USER_ID`、`REDIRECT_URI`、`EXPIRES_AT`、`USED_AT` | 判断 code 是否存在、过期、已使用或绑定信息不一致 |
| `CM_THIRDPART_ACCESS_TOKEN` | `TOKEN_HASH`、`CLIENT_ID`、`USER_ID`、`EXPIRES_AT`、`REVOKED_AT`、`LAST_USED_AT` | 判断 token 是否存在、过期、撤销或最近使用时间 |

当前默认授权码有效期为 5 分钟，access token 有效期为 2 小时。后续可扩展为环境变量配置，例如授权码 TTL 和 access token TTL；在代码实现对应环境变量前，维护文档和部署脚本不要把这些 TTL 写成已可配置项。

检查 H5 登录入口和本地资源：

```bash
curl -s -o /tmp/bpmt-h5-login.html -w '%{http_code}\n' 'http://127.0.0.1:8080/login.jsp?_action_mode=h5'
rg -n 'apps.bdimg.com|cdn.bootcss.com|cdn.bootcdn.net|res.wx.qq.com' /tmp/bpmt-h5-login.html
```

期望状态码为 `200`，且不出现外部 CDN 主机名。发布验收时还应使用移动视口登录 `admin/admin`，确认首页、菜单、首页面板可浏览。

检查日志目录：

```bash
find runtime/platform-logs -maxdepth 3 -type f | sort
find runtime/tomcat-logs -maxdepth 2 -type f | sort
find runtime/api-platform-logs -maxdepth 3 -type f | sort
find runtime/api-tomcat-logs -maxdepth 2 -type f | sort
```

`platform.log`、`script.log`、`sql/`、`stat/`、`perf4j/` 应落在 `runtime/platform-logs/`；Tomcat 自身日志应落在 `runtime/tomcat-logs/`。
API 容器对应日志分别落在 `runtime/api-platform-logs/` 和 `runtime/api-tomcat-logs/`。

## 发布流程

正式发布建议按以下顺序：

1. 新建发布分支
2. 修改 Maven 版本号
3. 修改 `docker-compose.yml` 默认 `BPMT_IMAGE_TAG` 和 `BPMT_API_IMAGE_TAG`
4. 更新 README 和 release 文档
5. 运行 `scripts/verify-repo.sh`
6. 使用 Java 8 编译
7. 执行 `scripts/build-image.sh`
8. 执行 `scripts/build-api-image.sh`
9. 执行 `scripts/build-multiarch-images.sh` 推送 Web/API multi-arch 镜像，并同步 `latest`
10. 使用 `docker buildx imagetools inspect` 确认 Web/API 均包含 `linux/amd64` 和 `linux/arm64`
11. 在 amd64 Linux 和 arm64 环境至少各完成一次拉取或 compose smoke
12. 基于 tag 或干净克隆运行 compose 验证
13. 合并 PR 到 `main`
14. 在合并后的 `main` 打 git tag
15. 创建 GitHub Release

## v1.1.0 发布候选验收

截至 2026-04-26，当前分支已完成一次 `v1.1.0` 发布候选验收：

- Java 8 + public-only 临时 Maven settings + 空本地 Maven 仓库：`mvn -s <tmp-settings> -DskipTests compile` 通过。
- `scripts/build-image.sh` 通过，生成本地镜像 `ghcr.io/wodenwang/bpmt-lite:1.1.0`。
- 使用 `database/bpmt-db.sql` 从零初始化 MariaDB 通过。
- 初始化后 `kyq` 表数量为 176，Activiti 24 张，Quartz 11 张，OAuth 登录表 3 张。
- `http://127.0.0.1:<test-port>/` 返回 200。
- `http://127.0.0.1:<test-port>/ueditor/` 返回 200。
- `config/overrides/page.properties` 覆盖已验证。
- 容器内默认 `log.properties` 为 `log.encoding=utf8`、`log.level=debug`。

## GHCR 发布

登录 GHCR：

```bash
gh auth token | docker login ghcr.io -u wodenwang --password-stdin
```

推送镜像：

```bash
docker push ghcr.io/wodenwang/bpmt-lite:<tag>
docker push ghcr.io/wodenwang/bpmt-lite-api:<tag>
```

匿名拉取验证：

```bash
TMP_DOCKER_CONFIG=$(mktemp -d)
DOCKER_CONFIG="$TMP_DOCKER_CONFIG" docker pull ghcr.io/wodenwang/bpmt-lite:<tag>
DOCKER_CONFIG="$TMP_DOCKER_CONFIG" docker pull ghcr.io/wodenwang/bpmt-lite-api:<tag>
rm -rf "$TMP_DOCKER_CONFIG"
```

如果需要分别验证多架构镜像，可以显式指定平台：

```bash
DOCKER_CONFIG="$TMP_DOCKER_CONFIG" docker pull --platform linux/amd64 ghcr.io/wodenwang/bpmt-lite:<tag>
DOCKER_CONFIG="$TMP_DOCKER_CONFIG" docker pull --platform linux/arm64 ghcr.io/wodenwang/bpmt-lite:<tag>
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

`db/init/*.sql` 只用于首次初始化。已有 `db/data` 时不会再次导入。

## v1.1.0 Office/PDF 转换裁剪

`v1.1.0` 默认发行不再包含 Aspose、JPedal 和 JODConverter。

影响范围：

- 不支持微信/企业微信文件自动转图文素材。
- 不支持非 PDF Office 附件在线转 PDF 预览。
- 不支持依赖 Office 服务的 PDF 导出。
- PDF 文件本身的普通上传、下载和直接预览不受影响。

默认 Docker 配置继续保持 `office.flag=false` 和 `office.prepare=false`。后续如果要恢复 Office/PDF 转换能力，应作为单独版本目标重新设计依赖来源、许可边界和运行服务。

## 常见问题

### 修改初始化 SQL 后为什么没有重新导入？

MariaDB 官方初始化机制只在数据目录为空时执行 `/docker-entrypoint-initdb.d`。需要重新导入时，先备份数据，再删除 `db/data` 后重启。

### 为什么不升级 Java、Tomcat、Spring 或 MariaDB？

本项目目标是发行方式现代化，不是技术栈升级。老技术栈虽然陈旧，但业务功能已经稳定；贸然升级会带来较高回归风险。

### 为什么 docker-compose.yml 只保留少量配置？

`v1.1.0` 起，默认 compose 只保留快速启动需要的端口、镜像 tag 和数据库连接信息。原 properties 的低频参数仍由 `docker/docker-entrypoint.sh` 生成默认值。`v1.3.0` 中 `DB_NAME` 默认连接 `bpmt`，可通过 `DB_NAME=bpmt_min docker compose up -d bpmt-web` 切换到最小库。

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
