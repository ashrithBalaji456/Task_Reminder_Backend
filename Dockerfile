# Stage 1: Build stage with Maven and OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime stage with Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app
USER appuser

# Expose default port
EXPOSE 8080

# Configure health check against Spring Boot health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Environment defaults
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
