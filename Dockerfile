# ---- 1단계: 빌드 (JDK 25) ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# 빌드 스크립트만 먼저 복사 → 의존성 다운로드를 레이어 캐시 (소스만 바뀌면 재다운로드 없음)
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 빌드 (테스트는 CI에서 돌므로 이미지 빌드에선 생략해 NAS 부담 최소화)
COPY src src
RUN ./gradlew --no-daemon build -x test

# ---- 2단계: 실행 (JRE 25, 경량) ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/gsmhs.xyz-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# --enable-native-access: sqlite-jdbc 네이티브 로드 경고 제거 (JDK 25)
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "app.jar"]
