# --- сборка -------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /src

# сначала только файлы сборки: слой с зависимостями переиспользуется, пока они не менялись
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies --configuration runtimeClasspath

COPY src src
RUN ./gradlew --no-daemon bootJar && mv build/libs/*.jar /src/app.jar

# --- запуск -------------------------------------------------------------------
FROM eclipse-temurin:25-jre

# бот ходит в Telegram сам, внутрь ему никто не стучится — порты не нужны
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin neighbors
USER neighbors

COPY --from=build /src/app.jar app.jar

# в контейнере фиксированный лимит памяти задавать нечем — считаем от доступной
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
