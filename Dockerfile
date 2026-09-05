# Multi-stage Dockerfile for Sapphire Charting API with OpenTelemetry

# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre

# Install curl for downloading OpenTelemetry agent
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -g 1001 -r appgroup && \
    useradd -u 1001 -r -g appgroup appuser

# Set working directory
WORKDIR /app

# Download OpenTelemetry Java agent
# Version can be overridden via build arg
ARG OTEL_VERSION=2.2.0
RUN curl -L -o /app/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_VERSION}/opentelemetry-javaagent.jar

# Copy the built JAR from build stage
COPY --from=build /app/target/sapphire-charting-api-*.jar /app/sapphire-charting-api.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8089

# Default JVM options (can be overridden via environment variables)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

# Run the application with OpenTelemetry agent
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -javaagent:/app/opentelemetry-javaagent.jar -jar /app/sapphire-charting-api.jar"]

# Labels for metadata
LABEL maintainer="sapphire-team@example.com"
LABEL version="1.0.0"
LABEL description="Sapphire Charting API with OpenTelemetry instrumentation"
LABEL org.opencontainers.image.source="https://github.com/your-org/sapphire-charting-api"

# Made with Bob
