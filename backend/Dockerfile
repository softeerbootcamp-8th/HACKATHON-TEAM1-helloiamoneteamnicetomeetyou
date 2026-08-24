# syntax=docker/dockerfile:1

# ---- Build stage ----
# gradle wrapper로 빌드해 로컬/CI/이미지의 Gradle 버전을 항상 일치시킨다.
#
# --platform=$BUILDPLATFORM 을 붙여 이 스테이지는 항상 빌더의 native 아키텍처로 돌린다.
# 배포 대상은 t4g.micro(arm64)지만 GitHub Actions 러너는 amd64라서, 이걸 안 붙이면
# Gradle 전체가 QEMU 에뮬레이션으로 돌아 빌드가 몇 배 느려진다.
# bootJar 산출물은 아키텍처 중립이라 런타임 스테이지로 그대로 넘겨도 문제없다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
# 의존성만 먼저 받아서 별도 레이어로 캐시한다 — 소스만 바뀌면 이 레이어는 재사용된다.
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
# 이 스테이지는 TARGETPLATFORM(배포 시 linux/arm64)으로 만들어진다.
#
# 여기에는 RUN 을 두지 않는다. RUN 이 하나라도 있으면 그 명령은 arm64 로 실행돼야 해서
# amd64 러너에서 QEMU(docker/setup-qemu-action) 셋업이 필요해진다.
# COPY/ENV/USER 만 쓰면 실행 없이 이미지 조립만 하므로 QEMU 자체가 불필요하다.
# 그래서 유저 생성(adduser) 대신 숫자 UID 를 그대로 쓰고, chown 은 COPY --chown 으로 처리한다.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# 글롭(*.jar)을 쓰지 않는다 — build.gradle 에서 bootJar 이름을 app.jar 로 고정했다.
# 글롭을 쓰면 -plain.jar 가 같이 생기는 순간(./gradlew build) COPY 가 깨진다.
COPY --from=build --chown=1000:1000 /workspace/build/libs/app.jar app.jar
USER 1000:1000

EXPOSE 8080

# EC2 배포 시 SPRING_PROFILES_ACTIVE=prod, 로컬 E2E는 docker-compose.yml에서 e2e로 지정한다.
# t4g.micro는 RAM이 1GB뿐이라 배포 시 JAVA_OPTS로 힙을 제한한다 (deploy.yml 참고).
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
