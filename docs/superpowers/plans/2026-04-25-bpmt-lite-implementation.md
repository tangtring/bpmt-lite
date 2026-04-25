# bpmt-lite 实施计划

> **给 agentic workers：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用 checkbox（`- [ ]`）语法追踪进度。

**目标：** 将旧 BPMT 平台整理为 `bpmt-lite`：最小 Maven 多模块源码、Tomcat 7 + Java 8 镜像、MariaDB compose 运行、ROOT + ueditor 部署、properties 由 compose 配置。

**架构：** 保留原 Java Web 应用和必要内部模块，不改业务功能。Docker 镜像只包含运行时和应用产物，运行配置由 compose 环境变量和可选 override 文件在容器启动时生成到 `WEB-INF/classes/*.properties`。

**技术栈：** Java 8、Maven、Tomcat 7、MariaDB 10.11、Docker Compose、Shell。

---

## 文件结构

实施完成后，仓库的核心文件职责如下：

- `.gitignore`：忽略运行数据、构建产物、密钥、本地 Maven settings、数据库备份。
- `README.md`：简体中文项目说明、使用者运行方式、维护者构建方式。
- `pom.xml`：根 Maven 聚合 POM，只包含最小模块。
- `settings.example.xml`：维护者 Maven settings 示例。
- `parent/`：原 Maven 父 POM 模块。
- `util/`：原工具模块。
- `magic/`：原 magic API 和实现模块。
- `dbtools/`：原数据库工具模块。
- `platform/`：原核心 Web WAR 模块。
- `docker/Dockerfile`：Tomcat 7 + Java 8 镜像定义。
- `docker/docker-entrypoint.sh`：启动时生成 properties 并启动 Tomcat。
- `docker-compose.yml`：最终使用者运行入口，默认拉取已发布镜像。
- `config/overrides/.gitkeep`：保留 properties 覆盖文件目录。
- `db/init/.gitkeep`：保留 `kyq.sql` 放置目录。
- `runtime/.gitkeep`：保留运行目录。
- `scripts/build-image.sh`：维护者构建镜像入口。
- `scripts/verify-repo.sh`：仓库卫生验证。
- `docs/superpowers/specs/2026-04-25-bpmt-lite-design.md`：已确认设计文档。
- `docs/superpowers/plans/2026-04-25-bpmt-lite-implementation.md`：本实施计划。

---

### Task 1: 仓库基础和忽略规则

**Files:**
- Create: `.gitignore`
- Create: `README.md`
- Create: `config/overrides/.gitkeep`
- Create: `db/init/.gitkeep`
- Create: `runtime/.gitkeep`

- [ ] **Step 1: 确认当前仓库状态**

Run:

```bash
git status --short --branch
git remote -v
```

Expected:

```text
## main
origin	https://github.com/wodenwang/bpmt-lite.git (fetch)
origin	https://github.com/wodenwang/bpmt-lite.git (push)
```

- [ ] **Step 2: 创建保留目录**

Run:

```bash
mkdir -p config/overrides db/init runtime scripts docker
touch config/overrides/.gitkeep db/init/.gitkeep runtime/.gitkeep
```

Expected: command exits `0`.

- [ ] **Step 3: 写入 `.gitignore`**

Write `.gitignore` with this content:

```gitignore
.DS_Store
.svn/

target/
**/target/
*.class

settings.local.xml
settings.xml

db/data/
db/logs/
db/init/*.sql
db/init/*.sql.gz

runtime/*
!runtime/.gitkeep

config/overrides/*
!config/overrides/.gitkeep

*.log
*.tmp
cookies.txt

*.war
*.ear
aspose-*.jar
jpedal*.jar
patch-implementation*.jar
ueditor*.war
```

- [ ] **Step 4: 写入 README 初稿**

Write `README.md` with this content:

````markdown
# bpmt-lite

`bpmt-lite` 是 BPMT 低代码平台的简化发行工程。BPMT 表示 BPM + table，核心能力是自定义工作流和动态表格。

本项目只调整代码结构、打包方式、配置方式和部署方式，不升级技术栈、不重写功能、不增加功能。运行栈继续保持 Java 8、Tomcat 7、MariaDB。

## 使用者运行

默认使用已发布 Docker 镜像：

```bash
docker compose up -d
```

如果需要初始化干净数据库，将 `kyq.sql` 放到：

```text
db/init/kyq.sql
```

MariaDB 只会在首次创建 `db/data` 数据目录时自动导入该文件。数据库已经初始化后，替换 `kyq.sql` 不会自动重新导入。

## 维护者构建

维护者需要 Java 8、Maven、Docker，以及可访问旧私有依赖的 Maven 仓库。

```bash
cp settings.example.xml settings.local.xml
scripts/build-image.sh
```

`settings.local.xml` 不提交到 git。

## 项目语言

本项目沟通和文档统一使用简体中文。代码、命令、配置键名、Maven 坐标、镜像名等技术标识保持原样。
````

- [ ] **Step 5: 提交仓库基础文件**

Run:

```bash
git add .gitignore README.md config/overrides/.gitkeep db/init/.gitkeep runtime/.gitkeep
git commit -m "工程：初始化仓库基础文件"
```

Expected: commit succeeds.

---

### Task 2: 迁入最小源码模块

**Files:**
- Modify: `pom.xml`
- Create: `parent/`
- Create: `util/`
- Create: `magic/`
- Create: `dbtools/`
- Create: `platform/`

- [ ] **Step 1: 设置源路径变量**

Run:

```bash
SRC=/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk
test -d "$SRC/platform/src/main/webapp/WEB-INF"
```

Expected: command exits `0`.

- [ ] **Step 2: 复制根 POM 和最小模块**

Run:

```bash
SRC=/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk

rsync -a --delete \
  --exclude '.svn/' \
  --exclude 'target/' \
  --exclude '.DS_Store' \
  "$SRC/pom.xml" ./pom.xml

rsync -a --delete --exclude '.svn/' --exclude 'target/' --exclude '.DS_Store' "$SRC/parent/" ./parent/
rsync -a --delete --exclude '.svn/' --exclude 'target/' --exclude '.DS_Store' "$SRC/util/" ./util/
rsync -a --delete --exclude '.svn/' --exclude 'target/' --exclude '.DS_Store' "$SRC/magic/" ./magic/
rsync -a --delete --exclude '.svn/' --exclude 'target/' --exclude '.DS_Store' "$SRC/dbtools/" ./dbtools/

rsync -a --delete \
  --exclude '.svn/' \
  --exclude 'target/' \
  --exclude '.DS_Store' \
  --exclude 'cookies.txt' \
  --exclude 'src/test/resources/' \
  --exclude 'src/main/docker/' \
  "$SRC/platform/" ./platform/
```

Expected: command exits `0`.

- [ ] **Step 3: 验证不包含旧发行模块和临时目录**

Run:

```bash
test ! -d package
test ! -d tools
test ! -d support
find . -path '*/.svn' -o -path '*/target' -o -name cookies.txt
```

Expected: `test` commands exit `0`; `find` prints no output.

- [ ] **Step 4: 验证没有迁入禁止的大文件和私有二进制**

Run:

```bash
find . -type f \( \
  -name 'kyq.sql' \
  -o -name 'aspose-*.jar' \
  -o -name 'jpedal*.jar' \
  -o -name 'patch-implementation*.jar' \
  -o -name 'ueditor*.war' \
\) -print
```

Expected: no output.

- [ ] **Step 5: 提交源码迁入**

Run:

```bash
git add pom.xml parent util magic dbtools platform
git commit -m "工程：迁入 bpmt 最小源码模块"
```

Expected: commit succeeds.

---

### Task 3: 收敛 Maven 聚合和本地 settings

**Files:**
- Modify: `pom.xml`
- Modify: `platform/pom.xml`
- Create: `settings.example.xml`

- [ ] **Step 1: 将根 POM 的模块收敛为最小模块**

In `pom.xml`, replace the full `<modules>` block with:

```xml
  <modules>
    <module>parent</module>
    <module>util</module>
    <module>magic</module>
    <module>dbtools</module>
    <module>platform</module>
  </modules>
```

Also keep the root Maven coordinates compatible with existing child POMs:

```xml
  <groupId>com.riversoft</groupId>
  <artifactId>riversoft-product</artifactId>
  <version>3.4.1-SNAPSHOT</version>
  <packaging>pom</packaging>
```

- [ ] **Step 2: 添加根级 `docker-image` profile**

In `pom.xml`, ensure this profile exists under `<profiles>`:

```xml
    <profile>
      <id>docker-image</id>
      <properties>
        <maven.test.skip>true</maven.test.skip>
        <maven.javadoc.skip>true</maven.javadoc.skip>
      </properties>
    </profile>
```

- [ ] **Step 3: 创建 `settings.example.xml`**

Write `settings.example.xml` with this content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>${user.home}/.m2/repository</localRepository>

  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Public</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <id>bpmt-lite-repositories</id>
      <repositories>
        <repository>
          <id>local-file-repo</id>
          <name>Local file repository for historical BPMT artifacts</name>
          <url>file:///Volumes/vm/maven/repository</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
        <repository>
          <id>maven-public</id>
          <name>RiverSoft historical Maven repository</name>
          <url>https://nexus.riversoft.com.cn/repository/maven-public/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
        <repository>
          <id>central</id>
          <name>Maven Central</name>
          <url>https://repo1.maven.org/maven2/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <name>Maven Central Plugins</name>
          <url>https://repo1.maven.org/maven2/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>bpmt-lite-repositories</activeProfile>
  </activeProfiles>
</settings>
```

- [ ] **Step 4: 检查 Maven 聚合模块**

Run:

```bash
cp settings.example.xml settings.local.xml
mvn -s settings.local.xml -q -N help:evaluate -Dexpression=project.artifactId -DforceStdout
mvn -s settings.local.xml -q -pl platform -am help:evaluate -Dexpression=project.version -DforceStdout
```

Expected first command prints:

```text
riversoft-product
```

Expected second command prints a `3.4.1-SNAPSHOT` value.

- [ ] **Step 5: 提交 Maven 收敛**

Run:

```bash
git add pom.xml platform/pom.xml settings.example.xml
git commit -m "构建：收敛 Maven 模块和 settings 示例"
```

Expected: commit succeeds.

---

### Task 4: 实现 Docker entrypoint 的 properties 生成

**Files:**
- Create: `docker/docker-entrypoint.sh`

- [ ] **Step 1: 写入 entrypoint 脚本**

Write `docker/docker-entrypoint.sh` with this content:

```sh
#!/bin/sh
set -eu

APP_ROOT="${APP_ROOT:-/usr/local/tomcat/webapps}"
APP_CLASSES="${APP_CLASSES:-$APP_ROOT/ROOT/WEB-INF/classes}"
CONFIG_OVERRIDE_DIR="${CONFIG_OVERRIDE_DIR:-/config/overrides}"

mkdir -p "$APP_CLASSES" "$APP_ROOT/attachment" "$APP_ROOT/download" "$APP_ROOT/logs" /usr/local/tomcat/logs

DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-kyq}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-123456}"
JDBC_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=UTF-8"

append_override() {
  file_name="$1"
  override_file="$CONFIG_OVERRIDE_DIR/$file_name"
  target_file="$APP_CLASSES/$file_name"
  if [ -f "$override_file" ]; then
    {
      printf '\n'
      printf '# override from %s\n' "$override_file"
      cat "$override_file"
      printf '\n'
    } >> "$target_file"
  fi
}

cat > "$APP_CLASSES/jdbc.properties" <<EOF
database.type=mysql
jdbc.driverClassName=com.mysql.jdbc.Driver
jdbc.url=${JDBC_URL}
jdbc.username=${DB_USER}
jdbc.password=${DB_PASSWORD}
jdbc.pool.partition.count=${JDBC_POOL_PARTITION_COUNT:-2}
jdbc.pool.partition.min=${JDBC_POOL_PARTITION_MIN:-5}
jdbc.pool.partition.max=${JDBC_POOL_PARTITION_MAX:-50}
hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
hibernate.autoupdate=${HIBERNATE_AUTOUPDATE:-false}
hibernate.cache=${HIBERNATE_CACHE:-true}
sql.log=${SQL_LOG:-false}
sql.stat.enable=${SQL_STAT_ENABLE:-false}
sql.stat.limited=${SQL_STAT_LIMITED:-500}
freemarker.delay=${FREEMARKER_DELAY:-3600}
EOF
append_override jdbc.properties

cat > "$APP_CLASSES/db.properties" <<EOF
db.def.driverClassName=com.mysql.jdbc.Driver
db.def.url=${JDBC_URL}
db.def.username=${DB_USER}
db.def.password=${DB_PASSWORD}
db.def.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
EOF
append_override db.properties

cat > "$APP_CLASSES/page.properties" <<EOF
page.title=${PAGE_TITLE:-BPMT}
page.theme=${PAGE_THEME:-smoothness}
page.theme.ext=${PAGE_THEME_EXT:-}
page.theme.backgroud=${PAGE_THEME_BACKGROUD:-0}
page.tips=${PAGE_TIPS:-}
page.logo.url=${PAGE_LOGO_URL:-}
page.copyright=${PAGE_COPYRIGHT:-copyright &copy; 2012-2016 Riversoft Designs}
page.randomcode=${PAGE_RANDOMCODE:-3}
page.taskpanel=${PAGE_TASKPANEL:-true}
page.browser.msg=${PAGE_BROWSER_MSG:-}
page.browser.url=${PAGE_BROWSER_URL:-}
page.frame.new=${PAGE_FRAME_NEW:-false}
page.frame.login=${PAGE_FRAME_LOGIN:-}
page.language=${PAGE_LANGUAGE:-zh_CN;en}
EOF
append_override page.properties

cat > "$APP_CLASSES/safe.properties" <<EOF
safe.role=${SAFE_ROLE:-LIGHT_WEIGHT}
safe.sync.threads=${SAFE_SYNC_THREADS:-10}
safe.white.ip=${SAFE_WHITE_IP:-}
safe.white.uid=${SAFE_WHITE_UID:-}
safe.admin=${SAFE_ADMIN:-admin}
EOF
append_override safe.properties

cat > "$APP_CLASSES/sms.properties" <<EOF
sms.ali.enable=${SMS_ALI_ENABLE:-false}
sms.verified.system=${SMS_VERIFIED_SYSTEM:-BPMT}
sms.verified.length=${SMS_VERIFIED_LENGTH:-6}
sms.verified.template.default=${SMS_VERIFIED_TEMPLATE_DEFAULT:-}
sms.ali.endpoint=${SMS_ALI_ENDPOINT:-https://eco.taobao.com/router/rest}
sms.ali.appKey=${SMS_ALI_APPKEY:-}
sms.ali.appSecret=${SMS_ALI_APPSECRET:-}
sms.ali.signName=${SMS_ALI_SIGNNAME:-}
EOF
append_override sms.properties

cat > "$APP_CLASSES/wx.properties" <<EOF
wx.web.login.qrcode=${WX_WEB_LOGIN_QRCODE:-false}
wx.web.appId=${WX_WEB_APPID:-}
wx.web.appSecret=${WX_WEB_APPSECRET:-}
wx.web.mp.appIds=${WX_WEB_MP_APPIDS:-}
wx.net.domain=${WX_NET_DOMAIN:-localhost}
wx.net.https=${WX_NET_HTTPS:-false}
wx.qy.flag=${WX_QY_FLAG:-false}
wx.qy.corpId=${WX_QY_CORPID:-}
wx.qy.corpSecret=${WX_QY_CORPSECRET:-}
wx.qy.contactmode=${WX_QY_CONTACTMODE:-0}
wx.qy.pay.flag=${WX_QY_PAY_FLAG:-false}
wx.qy.pay.mchId=${WX_QY_PAY_MCHID:-}
wx.qy.pay.key=${WX_QY_PAY_KEY:-}
wx.qy.pay.certPath=${WX_QY_PAY_CERTPATH:-}
wx.qy.pay.certPassword=${WX_QY_PAY_CERTPASSWORD:-}
wx.open.flag=${WX_OPEN_FLAG:-false}
wx.open.appId=${WX_OPEN_APPID:-}
wx.open.appSecret=${WX_OPEN_APPSECRET:-}
wx.open.table=${WX_OPEN_TABLE:-}
EOF
append_override wx.properties

MAIL_FLOW_SUBJECT_SCRIPT="${MAIL_FLOW_SUBJECT_SCRIPT:-[流程通知]\${fo.pdName}:\${fo.activityName}}"
MAIL_FLOW_CONTENT_SCRIPT="${MAIL_FLOW_CONTENT_SCRIPT:-}"
cat > "$APP_CLASSES/mail.properties" <<EOF
mail.receiver.host=${MAIL_RECEIVER_HOST:-}
mail.sender.host=${MAIL_SENDER_HOST:-}
mail.sender.account=${MAIL_SENDER_ACCOUNT:-}
mail.sender.password=${MAIL_SENDER_PASSWORD:-}
mail.notify.flag=${MAIL_NOTIFY_FLAG:-false}
mail.notify.user.setting=${MAIL_NOTIFY_USER_SETTING:-false}
mail.flow.subject.type=${MAIL_FLOW_SUBJECT_TYPE:-2}
mail.flow.content.type=${MAIL_FLOW_CONTENT_TYPE:-2}
mail.flow.subject.script=${MAIL_FLOW_SUBJECT_SCRIPT}
mail.flow.content.script=${MAIL_FLOW_CONTENT_SCRIPT}
EOF
append_override mail.properties

cat > "$APP_CLASSES/office.properties" <<EOF
office.flag=${OFFICE_FLAG:-false}
office.prepare=${OFFICE_PREPARE:-false}
office.port=${OFFICE_PORT:-2002}
office.file.size=${OFFICE_FILE_SIZE:-2}
office.upload.size=${OFFICE_UPLOAD_SIZE:-100}
office.installation.path=${OFFICE_INSTALLATION_PATH:-}
EOF
append_override office.properties

cat > "$APP_CLASSES/log.properties" <<EOF
log.encoding=${LOG_ENCODING:-UTF-8}
log.level=${LOG_LEVEL:-info}
log.jolbox.level=${LOG_JOLBOX_LEVEL:-warn}
log.3pp.level=${LOG_3PP_LEVEL:-warn}
log.keepdays=${LOG_KEEPDAYS:-30}
EOF
append_override log.properties

cat > "$APP_CLASSES/hazelcast.properties" <<EOF
hazelcast.group.name=${HAZELCAST_GROUP_NAME:-bpmt}
hazelcast.group.password=${HAZELCAST_GROUP_PASSWORD:-bpmt}
hazelcast.management.center.enable=${HAZELCAST_MANAGEMENT_CENTER_ENABLE:-false}
hazelcast.management.center.url=${HAZELCAST_MANAGEMENT_CENTER_URL:-http://localhost:8080/mancenter}
hazelcast.port=${HAZELCAST_PORT:-5701}
hazelcast.multicast=${HAZELCAST_MULTICAST:-false}
hazelcast.multicast.group=${HAZELCAST_MULTICAST_GROUP:-224.2.2.3}
hazelcast.multicast.port=${HAZELCAST_MULTICAST_PORT:-54327}
hazelcast.tcpip=${HAZELCAST_TCPIP:-false}
hazelcast.tcpip.members=${HAZELCAST_TCPIP_MEMBERS:-127.0.0.1}
EOF
append_override hazelcast.properties

cat > "$APP_CLASSES/activiti.properties" <<EOF
activiti.font=${ACTIVITI_FONT:-simsun}
EOF
append_override activiti.properties

cat > "$APP_CLASSES/redis.properties" <<EOF
redis.flag=${REDIS_FLAG:-false}
redis.ip=${REDIS_IP:-redis}
redis.port=${REDIS_PORT:-6379}
redis.maxTotal=${REDIS_MAXTOTAL:-5}
redis.maxIdle=${REDIS_MAXIDLE:-}
redis.minIdle=${REDIS_MINIDLE:-}
redis.maxWaitMillis=${REDIS_MAXWAITMILLIS:-}
redis.testOnBorrow=${REDIS_TESTONBORROW:-}
redis.testOnReturn=${REDIS_TESTONRETURN:-}
redis.timeout=${REDIS_TIMEOUT:-2000}
redis.password=${REDIS_PASSWORD:-}
redis.dbIndex=${REDIS_DBINDEX:-0}
EOF
append_override redis.properties

cat > "$APP_CLASSES/quartz.properties" <<EOF
quartz.threadPool.threadCount=${QUARTZ_THREADPOOL_THREADCOUNT:-5}
quartz.jobStore.class=${QUARTZ_JOBSTORE_CLASS:-org.quartz.impl.jdbcjobstore.JobStoreTX}
quartz.jobStore.driverDelegateClass=${QUARTZ_JOBSTORE_DRIVERDELEGATECLASS:-org.quartz.impl.jdbcjobstore.StdJDBCDelegate}
org.quartz.jobStore.selectWithLockSQL=${ORG_QUARTZ_JOBSTORE_SELECTWITHLOCKSQL:-}
EOF
append_override quartz.properties

exec "$@"
```

- [ ] **Step 2: 设置可执行权限并做 shell 语法检查**

Run:

```bash
chmod +x docker/docker-entrypoint.sh
sh -n docker/docker-entrypoint.sh
```

Expected: command exits `0`.

- [ ] **Step 3: 提交 entrypoint**

Run:

```bash
git add docker/docker-entrypoint.sh
git commit -m "运行：生成 Docker 启动配置文件"
```

Expected: commit succeeds.

---

### Task 5: 增加 Dockerfile 和 Maven 镜像构建 profile

**Files:**
- Create: `docker/Dockerfile`
- Modify: `platform/pom.xml`
- Create: `scripts/build-image.sh`

- [ ] **Step 1: 写入 Dockerfile**

Write `docker/Dockerfile` with this content:

```dockerfile
FROM tomcat:7.0.109-jdk8-openjdk

ENV TZ=Asia/Shanghai \
    APP_ROOT=/usr/local/tomcat/webapps \
    APP_CLASSES=/usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
    CONFIG_OVERRIDE_DIR=/config/overrides \
    JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

RUN rm -rf /usr/local/tomcat/webapps/*

COPY platform.war /tmp/platform.war
COPY ueditor.war /tmp/ueditor.war

RUN mkdir -p /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ueditor \
    && cd /usr/local/tomcat/webapps/ROOT \
    && jar -xf /tmp/platform.war \
    && cd /usr/local/tomcat/webapps/ueditor \
    && jar -xf /tmp/ueditor.war \
    && rm -f /tmp/platform.war /tmp/ueditor.war \
    && mkdir -p /usr/local/tomcat/webapps/attachment \
    && mkdir -p /usr/local/tomcat/webapps/download \
    && mkdir -p /usr/local/tomcat/webapps/logs \
    && mkdir -p /config/overrides

COPY docker-entrypoint.sh /usr/local/bin/bpmt-entrypoint.sh

RUN chmod +x /usr/local/bin/bpmt-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["bpmt-entrypoint.sh"]
CMD ["catalina.sh", "run"]
```

- [ ] **Step 2: 在 `platform/pom.xml` 中加入 Docker properties**

Inside `<properties>`, ensure these properties exist:

```xml
    <docker.context.dir>${project.build.directory}/docker</docker.context.dir>
    <docker.image.name>ghcr.io/wodenwang/bpmt-lite</docker.image.name>
    <docker.image.tag>${project.version}</docker.image.tag>
    <docker.platform>linux/amd64</docker.platform>
```

- [ ] **Step 3: 在 `platform/pom.xml` 中加入 Docker profile**

Under `<profiles>`, add this complete profile before the existing `patch` profile:

```xml
    <profile>
      <id>docker-image</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-antrun-plugin</artifactId>
            <executions>
              <execution>
                <id>prepare-docker-context</id>
                <phase>prepare-package</phase>
                <goals>
                  <goal>run</goal>
                </goals>
                <configuration>
                  <target>
                    <delete dir="${docker.context.dir}" />
                    <mkdir dir="${docker.context.dir}" />
                    <copy todir="${docker.context.dir}">
                      <fileset dir="${project.basedir}/../docker" />
                    </copy>
                  </target>
                </configuration>
              </execution>
              <execution>
                <id>build-docker-image</id>
                <phase>verify</phase>
                <goals>
                  <goal>run</goal>
                </goals>
                <configuration>
                  <target>
                    <copy file="${project.build.directory}/${project.build.finalName}.war" tofile="${docker.context.dir}/platform.war" />
                    <exec executable="docker" dir="${docker.context.dir}" failonerror="true">
                      <arg value="build" />
                      <arg value="--platform" />
                      <arg value="${docker.platform}" />
                      <arg value="-t" />
                      <arg value="${docker.image.name}:${docker.image.tag}" />
                      <arg value="." />
                    </exec>
                  </target>
                </configuration>
              </execution>
            </executions>
          </plugin>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <executions>
              <execution>
                <id>copy-docker-ueditor</id>
                <phase>package</phase>
                <goals>
                  <goal>copy</goal>
                </goals>
                <configuration>
                  <artifactItems>
                    <artifactItem>
                      <groupId>com.riversoft</groupId>
                      <artifactId>ueditor</artifactId>
                      <version>${ueditor.version}</version>
                      <type>war</type>
                      <overWrite>true</overWrite>
                      <outputDirectory>${docker.context.dir}</outputDirectory>
                      <destFileName>ueditor.war</destFileName>
                    </artifactItem>
                  </artifactItems>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
```

- [ ] **Step 4: 写入维护者构建脚本**

Write `scripts/build-image.sh` with this content:

```sh
#!/bin/sh
set -eu

if [ ! -f settings.local.xml ]; then
  cp settings.example.xml settings.local.xml
  printf '%s\n' '已创建 settings.local.xml，请确认 Maven 私有依赖仓库路径后重新运行。'
  exit 1
fi

JAVA_VERSION_OUTPUT="$(java -version 2>&1 | head -n 1)"
case "$JAVA_VERSION_OUTPUT" in
  *'"1.8.'*) ;;
  *)
    printf '%s\n' "当前 Java 版本不是 Java 8：$JAVA_VERSION_OUTPUT"
    printf '%s\n' '请切换到 Java 8 后重新运行。'
    exit 1
    ;;
esac

mvn -s settings.local.xml -pl platform -am -Pdocker-image verify
```

- [ ] **Step 5: 校验脚本语法**

Run:

```bash
chmod +x scripts/build-image.sh
sh -n scripts/build-image.sh
```

Expected: command exits `0`.

- [ ] **Step 6: 提交 Docker 构建入口**

Run:

```bash
git add docker/Dockerfile platform/pom.xml scripts/build-image.sh
git commit -m "构建：增加 Tomcat Docker 镜像构建入口"
```

Expected: commit succeeds.

---

### Task 6: 编写最终使用者 docker-compose

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: 写入 `docker-compose.yml`**

Write `docker-compose.yml` with this content:

```yaml
services:
  web:
    image: ghcr.io/wodenwang/bpmt-lite:${BPMT_IMAGE_TAG:-3.4.1-SNAPSHOT}
    platform: linux/amd64
    container_name: bpmt-lite-web
    depends_on:
      mariadb:
        condition: service_healthy
    restart: unless-stopped
    ports:
      - "${BPMT_HTTP_PORT:-8080}:8080"
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mariadb
      DB_PORT: 3306
      DB_NAME: kyq
      DB_USER: root
      DB_PASSWORD: 123456
      HIBERNATE_AUTOUPDATE: "false"
      HIBERNATE_CACHE: "true"
      JDBC_POOL_PARTITION_COUNT: 2
      JDBC_POOL_PARTITION_MIN: 5
      JDBC_POOL_PARTITION_MAX: 50
      SQL_LOG: "false"
      SQL_STAT_ENABLE: "false"
      SQL_STAT_LIMITED: 500
      FREEMARKER_DELAY: 3600
      PAGE_TITLE: BPMT
      PAGE_THEME: smoothness
      PAGE_THEME_EXT: ""
      PAGE_THEME_BACKGROUD: 0
      PAGE_TIPS: ""
      PAGE_LOGO_URL: ""
      PAGE_COPYRIGHT: "copyright &copy; 2012-2016 Riversoft Designs"
      PAGE_RANDOMCODE: 3
      PAGE_TASKPANEL: "true"
      PAGE_BROWSER_MSG: ""
      PAGE_BROWSER_URL: ""
      PAGE_FRAME_NEW: "false"
      PAGE_FRAME_LOGIN: ""
      PAGE_LANGUAGE: "zh_CN;en"
      SAFE_ROLE: LIGHT_WEIGHT
      SAFE_SYNC_THREADS: 10
      SAFE_WHITE_IP: ""
      SAFE_WHITE_UID: ""
      SAFE_ADMIN: admin
      REDIS_FLAG: "false"
      REDIS_IP: redis
      REDIS_PORT: 6379
      REDIS_MAXTOTAL: 5
      OFFICE_FLAG: "false"
      OFFICE_PREPARE: "false"
      OFFICE_PORT: 2002
      OFFICE_FILE_SIZE: 2
      OFFICE_UPLOAD_SIZE: 100
      OFFICE_INSTALLATION_PATH: ""
      MAIL_NOTIFY_FLAG: "false"
      MAIL_NOTIFY_USER_SETTING: "false"
      MAIL_RECEIVER_HOST: ""
      MAIL_SENDER_HOST: ""
      MAIL_SENDER_ACCOUNT: ""
      MAIL_SENDER_PASSWORD: ""
      MAIL_FLOW_SUBJECT_TYPE: 2
      MAIL_FLOW_CONTENT_TYPE: 2
      MAIL_FLOW_SUBJECT_SCRIPT: "[流程通知]$${fo.pdName}:$${fo.activityName}"
      MAIL_FLOW_CONTENT_SCRIPT: ""
      SMS_ALI_ENABLE: "false"
      SMS_VERIFIED_SYSTEM: BPMT
      SMS_VERIFIED_LENGTH: 6
      SMS_VERIFIED_TEMPLATE_DEFAULT: ""
      SMS_ALI_ENDPOINT: https://eco.taobao.com/router/rest
      SMS_ALI_APPKEY: ""
      SMS_ALI_APPSECRET: ""
      SMS_ALI_SIGNNAME: ""
      WX_WEB_LOGIN_QRCODE: "false"
      WX_WEB_APPID: ""
      WX_WEB_APPSECRET: ""
      WX_WEB_MP_APPIDS: ""
      WX_NET_DOMAIN: localhost
      WX_NET_HTTPS: "false"
      WX_QY_FLAG: "false"
      WX_QY_CORPID: ""
      WX_QY_CORPSECRET: ""
      WX_QY_CONTACTMODE: 0
      WX_QY_PAY_FLAG: "false"
      WX_QY_PAY_MCHID: ""
      WX_QY_PAY_KEY: ""
      WX_QY_PAY_CERTPATH: ""
      WX_QY_PAY_CERTPASSWORD: ""
      WX_OPEN_FLAG: "false"
      WX_OPEN_APPID: ""
      WX_OPEN_APPSECRET: ""
      WX_OPEN_TABLE: ""
      LOG_ENCODING: UTF-8
      LOG_LEVEL: info
      LOG_JOLBOX_LEVEL: warn
      LOG_3PP_LEVEL: warn
      LOG_KEEPDAYS: 30
      HAZELCAST_GROUP_NAME: bpmt
      HAZELCAST_GROUP_PASSWORD: bpmt
      HAZELCAST_MANAGEMENT_CENTER_ENABLE: "false"
      HAZELCAST_MANAGEMENT_CENTER_URL: http://localhost:8080/mancenter
      HAZELCAST_PORT: 5701
      HAZELCAST_MULTICAST: "false"
      HAZELCAST_MULTICAST_GROUP: 224.2.2.3
      HAZELCAST_MULTICAST_PORT: 54327
      HAZELCAST_TCPIP: "false"
      HAZELCAST_TCPIP_MEMBERS: 127.0.0.1
      ACTIVITI_FONT: simsun
      QUARTZ_THREADPOOL_THREADCOUNT: 5
      QUARTZ_JOBSTORE_CLASS: org.quartz.impl.jdbcjobstore.JobStoreTX
      QUARTZ_JOBSTORE_DRIVERDELEGATECLASS: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
      ORG_QUARTZ_JOBSTORE_SELECTWITHLOCKSQL: ""
    volumes:
      - ./config/overrides:/config/overrides:ro
      - ./runtime/attachment:/usr/local/tomcat/webapps/attachment
      - ./runtime/download:/usr/local/tomcat/webapps/download
      - ./runtime/platform-logs:/usr/local/tomcat/webapps/logs
      - ./runtime/tomcat-logs:/usr/local/tomcat/logs

  mariadb:
    image: mariadb:10.11
    platform: linux/amd64
    container_name: bpmt-lite-mariadb
    restart: unless-stopped
    ports:
      - "${BPMT_DB_PORT:-3306}:3306"
    environment:
      TZ: Asia/Shanghai
      MARIADB_ROOT_PASSWORD: 123456
      MARIADB_DATABASE: kyq
    volumes:
      - ./db/data:/var/lib/mysql
      - ./db/init:/docker-entrypoint-initdb.d
      - ./db/logs:/var/log/mysql
    entrypoint:
      - /bin/bash
      - -lc
      - |
        mkdir -p /var/log/mysql
        chown -R mysql:mysql /var/log/mysql
        exec docker-entrypoint.sh mariadbd \
          --character-set-server=utf8 \
          --collation-server=utf8_general_ci \
          --lower_case_table_names=1 \
          --max-allowed-packet=1G \
          --net-read-timeout=600 \
          --net-write-timeout=600 \
          --log-error=/var/log/mysql/mariadb.err \
          --slow-query-log=1 \
          --slow-query-log-file=/var/log/mysql/mariadb-slow.log
    healthcheck:
      test: ["CMD-SHELL", "mariadb-admin ping -h 127.0.0.1 -uroot -p$${MARIADB_ROOT_PASSWORD} --silent"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 30s
```

- [ ] **Step 2: 校验 compose 语法**

Run:

```bash
docker compose config >/tmp/bpmt-lite-compose.yml
```

Expected: command exits `0`.

- [ ] **Step 3: 提交 compose**

Run:

```bash
git add docker-compose.yml
git commit -m "部署：增加最终用户 compose 配置"
```

Expected: commit succeeds.

---

### Task 7: 增加仓库卫生验证脚本

**Files:**
- Create: `scripts/verify-repo.sh`

- [ ] **Step 1: 写入验证脚本**

Write `scripts/verify-repo.sh` with this content:

```sh
#!/bin/sh
set -eu

fail() {
  printf '%s\n' "ERROR: $1" >&2
  exit 1
}

test -d parent || fail "missing parent module"
test -d util || fail "missing util module"
test -d magic || fail "missing magic module"
test -d dbtools || fail "missing dbtools module"
test -d platform || fail "missing platform module"

test ! -d package || fail "package module must not be migrated"
test ! -d tools || fail "tools module must not be migrated"
test ! -d support || fail "support module must not be migrated"

if find . -path './.git' -prune -o -path '*/.svn' -print | grep -q .; then
  fail "SVN metadata found"
fi

if find . -path './.git' -prune -o -path '*/target' -print | grep -q .; then
  fail "Maven target directory found"
fi

if find . -path './.git' -prune -o -type f \( -name 'kyq.sql' -o -name 'aspose-*.jar' -o -name 'jpedal*.jar' -o -name 'patch-implementation*.jar' -o -name 'ueditor*.war' \) -print | grep -q .; then
  fail "forbidden database dump or private binary found"
fi

origin_url="$(git remote get-url origin)"
test "$origin_url" = "https://github.com/wodenwang/bpmt-lite.git" || fail "unexpected origin: $origin_url"

printf '%s\n' "OK: repository hygiene checks passed"
```

- [ ] **Step 2: 运行验证脚本**

Run:

```bash
chmod +x scripts/verify-repo.sh
scripts/verify-repo.sh
```

Expected:

```text
OK: repository hygiene checks passed
```

- [ ] **Step 3: 提交验证脚本**

Run:

```bash
git add scripts/verify-repo.sh
git commit -m "验证：增加仓库卫生检查脚本"
```

Expected: commit succeeds.

---

### Task 8: 执行 Maven 与 Docker 构建验证

**Files:**
- Modify only if validation exposes a concrete build error: `pom.xml`, `parent/pom.xml`, `platform/pom.xml`

- [ ] **Step 1: 确认 Java 8**

Run:

```bash
java -version
```

Expected stderr first line contains:

```text
version "1.8.
```

- [ ] **Step 2: 运行 Maven 编译验证**

Run:

```bash
mvn -s settings.local.xml -pl platform -am -Pdocker-image -DskipTests compile
```

Expected: Maven exits `0`.

- [ ] **Step 3: 运行 Docker 镜像构建**

Run:

```bash
scripts/build-image.sh
```

Expected:

```text
Successfully built
```

or Docker BuildKit equivalent output ending with image tag `ghcr.io/wodenwang/bpmt-lite:3.4.1-SNAPSHOT`.

- [ ] **Step 4: 验证镜像内 webapp 布局**

Run:

```bash
docker run --rm --entrypoint sh ghcr.io/wodenwang/bpmt-lite:3.4.1-SNAPSHOT -c 'test -d /usr/local/tomcat/webapps/ROOT && test -d /usr/local/tomcat/webapps/ueditor && test -x /usr/local/bin/bpmt-entrypoint.sh'
```

Expected: command exits `0`.

- [ ] **Step 5: 提交构建修复**

If Steps 2-4 required POM changes, commit them:

```bash
git add pom.xml parent/pom.xml platform/pom.xml
git commit -m "构建：修复 Java 8 Docker 镜像构建"
```

Expected: commit succeeds only when files changed. If no files changed, skip this commit.

---

### Task 9: 执行 compose 启动和运行验证

**Files:**
- Modify only if validation exposes a concrete runtime wiring error: `docker-compose.yml`, `docker/docker-entrypoint.sh`, `README.md`

- [ ] **Step 1: 启动服务**

Run:

```bash
docker compose up -d
```

Expected: command exits `0`.

- [ ] **Step 2: 检查服务状态**

Run:

```bash
docker compose ps
```

Expected: `bpmt-lite-mariadb` is healthy and `bpmt-lite-web` is running.

- [ ] **Step 3: 验证 properties 已生成**

Run:

```bash
docker compose exec web sh -lc 'test -f /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/page.properties && grep "^page.title=BPMT" /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/page.properties && grep "^jdbc.url=jdbc:mysql://mariadb:3306/kyq" /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/jdbc.properties'
```

Expected: command exits `0` and prints matching `page.title` and `jdbc.url` lines.

- [ ] **Step 4: 验证 ROOT 和 ueditor 路径**

Run:

```bash
curl -fsSI http://127.0.0.1:${BPMT_HTTP_PORT:-8080}/
curl -fsSI http://127.0.0.1:${BPMT_HTTP_PORT:-8080}/ueditor/
```

Expected: each command exits `0` with an HTTP status header.

- [ ] **Step 5: 验证持久化目录**

Run:

```bash
test -d runtime/attachment
test -d runtime/download
test -d runtime/platform-logs
test -d runtime/tomcat-logs
test -d db/logs
ls db/logs
```

Expected: all `test` commands exit `0`; `ls db/logs` shows at least one MariaDB log file after MariaDB has started.

- [ ] **Step 6: 提交运行修复**

If Steps 1-5 required runtime config changes, commit them:

```bash
git add docker-compose.yml docker/docker-entrypoint.sh README.md
git commit -m "运行：修复 compose 启动和配置生成"
```

Expected: commit succeeds only when files changed. If no files changed, skip this commit.

---

### Task 10: 完成中文文档和最终仓库检查

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-04-25-bpmt-lite-design.md` only if implementation reveals a design correction.

- [ ] **Step 1: 补充 README 运行目录说明**

In `README.md`, add this section after "使用者运行":

````markdown
## 运行目录

运行时目录约定如下：

```text
db/init/kyq.sql        首次初始化数据库备份，不提交 git
db/data/               MariaDB 数据目录，不提交 git
db/logs/               MariaDB 日志目录，不提交 git
runtime/attachment/    BPMT 附件目录，不提交 git
runtime/download/      BPMT 下载目录，不提交 git
runtime/platform-logs/ BPMT 平台日志目录，不提交 git
runtime/tomcat-logs/   Tomcat 日志目录，不提交 git
config/overrides/      properties 覆盖文件目录，不提交具体覆盖文件
```

`config/overrides/*.properties` 会追加到容器启动时生成的同名 properties 文件后面，因此覆盖文件中的同名 key 优先级更高。
````

- [ ] **Step 2: 运行最终仓库检查**

Run:

```bash
scripts/verify-repo.sh
git status --short --branch
```

Expected:

```text
OK: repository hygiene checks passed
## main
```

- [ ] **Step 3: 提交最终文档**

Run:

```bash
git add README.md docs/superpowers/specs/2026-04-25-bpmt-lite-design.md
git commit -m "文档：补充 bpmt-lite 运行和维护说明"
```

Expected: commit succeeds only when files changed. If no files changed, skip this commit.

- [ ] **Step 4: 展示提交历史**

Run:

```bash
git log --oneline --decorate -10
```

Expected: latest commits show repository setup, source migration, build, runtime, validation, and documentation work.

---

## 自审记录

- 设计文档中的最小模块、ROOT + ueditor、MariaDB、compose 配置、properties 生成、volume 持久化、MariaDB 日志、私有依赖处理、中文文档约定均有对应任务。
- 本计划不包含业务功能扩展，不包含技术栈升级。
- 本计划保留旧 Java Web 运行模型，通过 Docker entrypoint 做配置桥接。
- 每个可提交阶段都有独立 commit 点，便于回滚和审查。
