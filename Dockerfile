# --- сборка -------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build

WORKDIR /src

# В gradle.properties проекта стоит -Xmx2g — это для разработки на машине с запасом памяти.
# На сервере такой демон убивает OOM-killer («daemon has disappeared»), поэтому урезаем
# аппетиты: настройки в GRADLE_USER_HOME приоритетнее проектных. in-process у Kotlin
# избавляет от форка ещё одной JVM под компилятор.
RUN mkdir -p /root/.gradle && printf '%s\n' \
    'org.gradle.jvmargs=-Xmx640m -XX:MaxMetaspaceSize=320m' \
    'org.gradle.daemon=false' \
    'org.gradle.parallel=false' \
    'org.gradle.caching=false' \
    'kotlin.compiler.execution.strategy=in-process' \
    > /root/.gradle/gradle.properties

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
