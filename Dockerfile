## 1. Build stage
#FROM eclipse-temurin:17-jdk-jammy AS build
#WORKDIR /app
#
## Gradle 래퍼와 설정 파일들을 먼저 복사 (캐시 효율화)
#COPY gradlew .
#COPY gradle gradle
#COPY build.gradle .
#COPY settings.gradle .
#
## 소스 코드 복사
#COPY src src
#
## 빌드 실행 (상세 로그 확인을 위해 --info 추가 가능)
#RUN ./gradlew clean bootJar -x test --no-daemon

# 2. Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
#COPY --from=build /app/build/libs/*.jar app.jar
# 로컬에서 빌드한 jar를 복사 (네트워크 불필요)
COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]