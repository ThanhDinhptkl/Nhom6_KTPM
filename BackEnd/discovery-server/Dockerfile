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

# Eureka Server Configuration
ENV SPRING_APPLICATION_NAME=discovery-server
ENV SERVER_PORT=8761
ENV EUREKA_INSTANCE_HOSTNAME=14.225.212.208
ENV EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
ENV EUREKA_CLIENT_FETCH_REGISTRY=false
ENV EUREKA_SERVER_ENABLE_SELF_PRESERVATION=true

# Create directory for logs
RUN mkdir -p /app/logs && chmod 777 /app/logs

EXPOSE 8761
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 
