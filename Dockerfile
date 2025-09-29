FROM gradle:8.5-jdk17 AS build
LABEL authors="c7h12"

# Arbeitsverzeichnis setzen
WORKDIR /app

# Gradle Wrapper und Dependencies kopieren
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# WICHTIG: Ausführungsrechte für gradlew setzen
RUN chmod +x gradlew

# Dependencies herunterladen (für besseres Caching)
RUN ./gradlew dependencies --no-daemon || true

# Source Code kopieren
COPY src src

# Application bauen
RUN ./gradlew bootJar --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine

# Arbeitsverzeichnis setzen
WORKDIR /app

# JAR aus build stage kopieren
COPY --from=build /app/build/libs/*.jar app.jar

# Port exponieren (Spring Boot default)
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Application starten
ENTRYPOINT ["java", "-jar", "app.jar"]