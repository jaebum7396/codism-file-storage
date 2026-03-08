# codism-file-storage Dockerfile (Spring Boot)
# 빌드 스테이지
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Gradle wrapper와 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐싱 최적화)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# 빌드 (테스트 제외, SNAPSHOT 의존성 갱신)
RUN ./gradlew clean build -x test --no-daemon --refresh-dependencies

# JAR 파일 추출
RUN find build/libs -name "*.jar" -not -name "*-plain.jar" -exec cp {} /app.jar \;

# 런타임 스테이지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드 스테이지에서 JAR 복사
COPY --from=builder /app.jar /app.jar

# 파일 저장 디렉토리 생성
RUN mkdir -p /data/files/temp /data/files/thumbnails

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app.jar"]
