# syntax=docker/dockerfile:1

# ---- Build: compile the jar and split it into layers ----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first: this layer is cached until pom.xml changes, so code-only changes build much faster.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
# Tests run before pushing (mvn clean test); the image build only packages.
RUN mvn -q -B package -DskipTests \
 && cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --destination /app

# ---- Run: a small Java 21 runtime, no build tools, not running as root -----------------------------------------
FROM eclipse-temurin:21-jre-noble
RUN groupadd --system lavrel && useradd --system --gid lavrel --no-create-home lavrel
WORKDIR /app
COPY --from=build --chown=lavrel:lavrel /app /app
USER lavrel

# Sized for small containers (Render Free: 0.1 vCPU, 512 MB): the heap takes 75 % of the container's memory, the serial
# collector suits a single CPU, and an out-of-memory error stops the container so the platform restarts it.
# Override at deploy time with JAVA_TOOL_OPTIONS if needed.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# The host sends the port in PORT (Render: 10000; application.yml: server.port: ${PORT:8080}).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
