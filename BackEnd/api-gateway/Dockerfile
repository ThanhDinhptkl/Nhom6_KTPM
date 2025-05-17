# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Application Configuration
ENV SPRING_APPLICATION_NAME=api-gateway
ENV SERVER_PORT=8080

# Eureka Client Configuration
ENV EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://14.225.212.208:8761/eureka/
ENV EUREKA_CLIENT_ENABLED=true
ENV EUREKA_INSTANCE_PREFER_IP_ADDRESS=true

# Create directory for logs
RUN mkdir -p /app/logs && chmod 777 /app/logs

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
