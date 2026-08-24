# syntax=docker/dockerfile:1

# ---- Build stage ----
# gradle wrapper로 빌드해 로컬/CI/이미지의 Gradle 버전을 항상 일치시킨다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
# 의존성만 먼저 받아서 별도 레이어로 캐시한다 — 소스만 바뀌면 이 레이어는 재사용된다.
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# EC2 배포 시 SPRING_PROFILES_ACTIVE=prod, 로컬 E2E는 docker-compose.yml에서 e2e로 지정한다.
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
