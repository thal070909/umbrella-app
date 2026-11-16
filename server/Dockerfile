# 1단계: 테스트를 통과해야 jar를 만든다
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon test bootJar

# 2단계: 실행에 필요한 JRE와 jar만 담는다
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 umbrella && mkdir -p /app/data && chown umbrella /app/data
COPY --from=build /workspace/build/libs/umbrella-server-*.jar app.jar
USER umbrella

# 무더위쉼터 저장 파일. 볼륨으로 붙이면 다시 배포해도 API 하루 한도를 쓰지 않는다
ENV SHELTER_SNAPSHOT_PATH=/app/data/shelters.json
VOLUME /app/data

EXPOSE 8080
# 상태 확인은 배포 환경에서 GET /actuator/health 로 한다
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
