# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM tomcat:10.1-jre17-temurin
ENV TZ=Asia/Ho_Chi_Minh \
    HRM_LOG_DIR=/usr/local/tomcat/logs \
    CATALINA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh -Dfile.encoding=UTF-8 -XX:MaxRAMPercentage=75"

RUN rm -rf /usr/local/tomcat/webapps/* \
 && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>\n      </Host>#' \
        /usr/local/tomcat/conf/server.xml \
 && groupadd --system hrm \
 && useradd --system --gid hrm --home-dir /usr/local/tomcat --shell /usr/sbin/nologin hrm \
 && chown -R hrm:hrm /usr/local/tomcat/webapps /usr/local/tomcat/work /usr/local/tomcat/temp /usr/local/tomcat/logs

COPY --from=build --chown=hrm:hrm /src/target/hrm.war /usr/local/tomcat/webapps/ROOT.war

USER hrm
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/health > /dev/null || exit 1
