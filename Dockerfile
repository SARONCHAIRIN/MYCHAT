FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/maven-wrapper.properties
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/main ./src/main
RUN ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 chat \
    && useradd --uid 10001 --gid chat --no-log-init --system \
        --home-dir /app --shell /usr/sbin/nologin chat \
    && install -d -o chat -g chat /app/uploads

WORKDIR /app
COPY --from=build /build/target/*.jar /app/application.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/app/uploads

USER 10001:10001
VOLUME ["/app/uploads"]
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent --show-error --max-time 4 \
        "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" --output /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
