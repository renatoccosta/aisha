# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-25 AS builder
WORKDIR /workspace

ARG REVISION=0.0.1-SNAPSHOT

COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
RUN chmod +x mvnw && ./mvnw -q -Drevision=${REVISION} -DskipTests dependency:go-offline

COPY src ./src
COPY docs/legal ./docs/legal
RUN ./mvnw -q -Drevision=${REVISION} -DskipTests package \
    && mkdir -p target/layers/dependencies target/layers/snapshot-dependencies target/layers/spring-boot-loader target/layers/application \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/layers

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system aisha \
    && useradd --system --gid aisha --create-home aisha \
    && mkdir -p /var/lib/aisha/backups \
    && chown -R aisha:aisha /var/lib/aisha

ENV TZ=UTC
ENV JAVA_OPTS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

COPY --from=builder /workspace/target/layers/dependencies/ ./
COPY --from=builder /workspace/target/layers/snapshot-dependencies/ ./
COPY --from=builder /workspace/target/layers/spring-boot-loader/ ./
COPY --from=builder /workspace/target/layers/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl --fail --silent http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

USER aisha

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
