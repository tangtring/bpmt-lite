#!/bin/sh
set -eu

APP_ROOT="${APP_ROOT:-/usr/local/tomcat/webapps}"
APP_CLASSES="${APP_CLASSES:-$APP_ROOT/ROOT/WEB-INF/classes}"
CONFIG_OVERRIDE_DIR="${CONFIG_OVERRIDE_DIR:-/config/overrides}"
TOMCAT_LOGS="${TOMCAT_LOGS:-/usr/local/tomcat/logs}"

mkdir -p "$APP_CLASSES" "$APP_ROOT/attachment" "$APP_ROOT/download" "$APP_ROOT/logs" "$TOMCAT_LOGS"

DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-bpmt}"
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

: > "$APP_CLASSES/production.properties"
append_override production.properties

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
page.copyright=${PAGE_COPYRIGHT:-Copyright (c) 2026 wodenwang and borballzhai}
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

if [ "${MAIL_FLOW_SUBJECT_SCRIPT+x}" != x ]; then
  MAIL_FLOW_SUBJECT_SCRIPT='[\u00c1\u00f7\u00b3\u00cc\u00cd\u00a8\u00d6\u00aa]${fo.pdName}:${fo.activityName}'
fi
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
log.encoding=${LOG_ENCODING:-utf8}
log.level=${LOG_LEVEL:-debug}
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
