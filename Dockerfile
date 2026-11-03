# syntax=docker/dockerfile:1
# -----------------------------------------------------------------------------
# HRM System – multi-stage image
#   build   : Maven + JDK 17 compiles the WAR (tests run in CI, not here)
#   runtime : Tomcat 10.1 on JRE 17, non-root, app deployed as ROOT (http://host:8080/)
# Configuration comes only from environment variables (HRM_DB_URL, HRM_DB_USER,
# HRM_DB_PASSWORD, HRM_API_KEY, …) – the image contains no credentials.
# -----------------------------------------------------------------------------

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
# dependencies first: this layer is reused as long as pom.xml does not change
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM tomcat:10.1-jre17-temurin
ENV TZ=Asia/Ho_Chi_Minh \
    HRM_LOG_DIR=/usr/local/tomcat/logs \
    CATALINA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh -Dfile.encoding=UTF-8 -XX:MaxRAMPercentage=75"

# no sample apps; Tomcat's own error pages must not reveal the server version;
# run as an unprivileged user that can only write where Tomcat needs to
RUN rm -rf /usr/local/tomcat/webapps/* \
 && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>\n      </Host>#' \
        /usr/local/tomcat/conf/server.xml \
 && groupadd --system hrm \
 && useradd --system --gid hrm --home-dir /usr/local/tomcat --shell /usr/sbin/nologin hrm \
 && chown -R hrm:hrm /usr/local/tomcat/webapps /usr/local/tomcat/work /usr/local/tomcat/temp /usr/local/tomcat/logs

COPY --from=build --chown=hrm:hrm /src/target/hrm.war /usr/local/tomcat/webapps/ROOT.war

USER hrm
EXPOSE 8080
# /health answers 503 while the database is unreachable, so "healthy" means "ready to serve"
HEALTHCHECK --interval=15s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/health > /dev/null || exit 1
