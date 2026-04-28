FROM eclipse-temurin:8-jdk-jammy

ARG TOMCAT_VERSION=7.0.109
ARG TOMCAT_DOWNLOAD_URL=https://archive.apache.org/dist/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz
ARG APT_MIRROR=https://mirrors.aliyun.com/ubuntu-ports

ENV TZ=Asia/Shanghai \
    CATALINA_HOME=/usr/local/tomcat \
    CATALINA_BASE=/usr/local/tomcat \
    APP_ROOT=/usr/local/tomcat/webapps \
    APP_CLASSES=/usr/local/tomcat/webapps/ROOT/WEB-INF/classes \
    CONFIG_OVERRIDE_DIR=/config/overrides \
    JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8" \
    PATH=/usr/local/tomcat/bin:$PATH

ADD ${TOMCAT_DOWNLOAD_URL} /tmp/tomcat.tar.gz

RUN if [ -n "$APT_MIRROR" ]; then sed -i "s|http://ports.ubuntu.com/ubuntu-ports|$APT_MIRROR|g" /etc/apt/sources.list; fi \
    && apt-get -o Acquire::Retries=3 update \
    && apt-get -o Acquire::Retries=3 install -y --no-install-recommends fontconfig fonts-wqy-zenhei \
    && fc-cache -f \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/local/tomcat \
    && tar -xzf /tmp/tomcat.tar.gz --strip-components=1 -C /usr/local/tomcat \
    && rm -f /tmp/tomcat.tar.gz \
    && rm -rf /usr/local/tomcat/webapps/* \
    && chmod +x /usr/local/tomcat/bin/*.sh

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
