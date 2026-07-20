# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress -DskipTests package \
    && cp "$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)" \
        /workspace/application.jar

FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache wget \
    && addgroup --system --gid 10001 app \
    && adduser --system --disabled-password --no-create-home \
        --uid 10001 --ingroup app app

WORKDIR /app

COPY --from=build --chown=10001:10001 /workspace/application.jar ./application.jar

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --timeout=5 --output-document=/dev/null \
        "http://127.0.0.1:${PORT:-8080}/actuator/health/readiness" || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/application.jar"]
