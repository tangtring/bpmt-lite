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
