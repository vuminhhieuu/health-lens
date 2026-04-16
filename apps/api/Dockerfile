# ============================================
# HealthLens API - Spring Boot Dockerfile
# Multi-stage build for optimized production image
# ============================================
#
# Build stages:
#   1. deps    - Download dependencies (cached)
#   2. builder - Compile and create JAR
#   3. runner  - Minimal runtime image
#
# Build context: Project root (where docker-compose.yml is)
#
# Usage:
#   docker build -t healthlens-api:latest -f apps/api/Dockerfile .
#   docker build --target builder -t healthlens-api:dev -f apps/api/Dockerfile .

# ============================================
# Stage 1: Dependencies
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /app

# Configure Gradle to use a local home inside the container to enable cross-stage caching
ENV GRADLE_USER_HOME=/app/gradle_home

# Copy gradle files from apps/api/ relative to build context
COPY apps/api/gradle apps/api/gradle
COPY apps/api/gradlew apps/api/gradlew
COPY apps/api/build.gradle.kts apps/api/build.gradle.kts
COPY apps/api/settings.gradle.kts apps/api/settings.gradle.kts
COPY apps/api/gradle.properties apps/api/gradle.properties

# Download dependencies (including Gradle itself)
RUN chmod +x apps/api/gradlew \
    && cd apps/api \
    && ./gradlew dependencies --no-daemon --console=plain

# ============================================
# Stage 2: Builder
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Ensure we use the same Gradle home
ENV GRADLE_USER_HOME=/app/gradle_home

# Copy gradle files
COPY apps/api/gradle apps/api/gradle
COPY apps/api/gradlew apps/api/gradlew
COPY apps/api/build.gradle.kts apps/api/build.gradle.kts
COPY apps/api/settings.gradle.kts apps/api/settings.gradle.kts
COPY apps/api/gradle.properties apps/api/gradle.properties

# Copy cached dependencies and Gradle distribution from deps stage
COPY --from=deps /app/gradle_home /app/gradle_home

# Copy source code and build
COPY apps/api/src apps/api/src
RUN chmod +x apps/api/gradlew \
    && cd apps/api \
    && ./gradlew bootJar --no-daemon --console=plain \
    && ls -la build/libs/

# ============================================
# Stage 3: Runner (Production)
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Security: Create non-root user
RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

# Install minimal runtime tools
RUN apk add --no-cache wget curl \
    && rm -rf /var/cache/apk/*

# Copy JAR from builder (in apps/api subdirectory)
COPY --from=builder /app/apps/api/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
# Render will map this to public 80 or 443
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 -q -O- http://localhost:8080/actuator/health/liveness \
    || curl -f http://localhost:8080/actuator/health/liveness \
    || exit 1

# JVM tuning
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Dio.netty.handler.ssl.noOpenSsl=true"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
