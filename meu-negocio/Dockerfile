# Imagem de produção (Railway). Ver planejamento_deploy_railway.md.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Wrapper e scripts primeiro para aproveitar o cache das dependências.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null
COPY config config
COPY src src
# bootJar e não build: build depende de installGitHooks, que roda git config e falha sem .git.
RUN ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Sao_Paulo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
