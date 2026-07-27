# syntax=docker/dockerfile:1

# ── build ─────────────────────────────────────────────────────────────
# 멀티모듈이라 settings.gradle이 참조하는 모든 모듈의 build.gradle을 먼저 복사해
# 의존성 해석 레이어를 소스와 분리한다. 소스만 바뀌면 의존성 다운로드를 건너뛴다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
COPY api/build.gradle           api/build.gradle
COPY core/build.gradle          core/build.gradle
COPY client/build.gradle        client/build.gradle
COPY mq/build.gradle            mq/build.gradle
COPY storage/db/build.gradle    storage/db/build.gradle
COPY storage/redis/build.gradle storage/redis/build.gradle

RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY . .
# 테스트는 CI에서 돌린다. 이미지 빌드는 산출물 생성만 담당한다.
RUN ./gradlew --no-daemon :api:bootJar -x test

# ── runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# healthcheck용 curl. 이미지 크기보다 기동 판정 신뢰성을 택한다.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# root로 돌리지 않는다. 컨테이너 탈출 시 피해 범위를 줄인다.
RUN useradd --system --create-home --uid 10001 gm
USER gm

COPY --from=build --chown=gm:gm /workspace/api/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

EXPOSE 8080

# exec 형태로 띄워야 PID 1이 자바가 되어 SIGTERM에 정상 종료한다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
