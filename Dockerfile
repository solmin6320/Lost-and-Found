# 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 먼저 받아서 캐시
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src
RUN ./gradlew bootJar --no-daemon

# 실행 - JRE만
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]