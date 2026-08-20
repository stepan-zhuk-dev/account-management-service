FROM gradle:8.14.3-jdk21 AS build

USER gradle
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle settings.gradle build.gradle ./

RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000,sharing=locked \
    gradle \
        --no-daemon \
        --console=plain \
        dependencies

COPY --chown=gradle:gradle src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000,sharing=locked \
    gradle \
        --no-daemon \
        --console=plain \
        bootJar \
        -x test

RUN set -eux; \
    JAR_FILE="$(find build/libs \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name '*-plain.jar' \
        -print \
        -quit)"; \
    test -n "${JAR_FILE}"; \
    jar tf "${JAR_FILE}" \
        | grep -q 'BOOT-INF/classes/db/migration/V1__create_banking_schema.sql'; \
    cp "${JAR_FILE}" /tmp/application.jar

RUN java \
    -Djarmode=tools \
    -jar /tmp/application.jar \
    extract \
    --layers \
    --destination /tmp/extracted

FROM eclipse-temurin:21-jre-jammy AS runtime

ARG APP_UID=10001
ARG APP_GID=10001

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd \
        --system \
        --gid "${APP_GID}" \
        spring \
    && useradd \
        --system \
        --uid "${APP_UID}" \
        --gid spring \
        --home-dir /app \
        --no-create-home \
        --no-log-init \
        --shell /usr/sbin/nologin \
        spring

WORKDIR /app

COPY --from=build --chown=spring:spring \
    /tmp/extracted/dependencies/ ./

COPY --from=build --chown=spring:spring \
    /tmp/extracted/spring-boot-loader/ ./

COPY --from=build --chown=spring:spring \
    /tmp/extracted/snapshot-dependencies/ ./

COPY --from=build --chown=spring:spring \
    /tmp/extracted/application/ ./

# Tuned for a container with a defined memory limit:
#
# - G1 provides balanced throughput and predictable latency.
# - 50% initial heap avoids excessive heap resizing after startup.
# - 75% maximum leaves 25% for native JVM memory and threads.
# - AlwaysPreTouch commits the initial heap during startup rather than introducing page faults during request processing.
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC \
-XX:+ParallelRefProcEnabled \
-XX:InitialRAMPercentage=50.0 \
-XX:MaxRAMPercentage=75.0 \
-XX:+AlwaysPreTouch \
-XX:+ExitOnOutOfMemoryError \
-Dfile.encoding=UTF-8 \
-Djava.io.tmpdir=/tmp"

USER spring:spring

EXPOSE 8080

STOPSIGNAL SIGTERM

HEALTHCHECK \
    --interval=15s \
    --timeout=3s \
    --start-period=60s \
    --retries=5 \
    CMD curl \
        --fail \
        --silent \
        --show-error \
        --max-time 2 \
        --output /dev/null \
        http://localhost:8080/actuator/health/liveness \
        || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
