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

# Add wait-for-it script to handle database connection delays
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Application Configuration
ENV SPRING_APPLICATION_NAME=BookingService
ENV SERVER_PORT=8082

# Database Configuration - will be overridden by docker-compose
ENV SPRING_DATASOURCE_URL=jdbc:mysql://booking-db:3306/booking_db?createDatabaseIfNotExist=true
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=root
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver

# Hibernate Configuration
ENV SPRING_JPA_SHOW_SQL=true
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Eureka Client Configuration
ENV EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://14.225.212.208:8761/eureka/
ENV EUREKA_CLIENT_ENABLED=true
ENV EUREKA_INSTANCE_PREFER_IP_ADDRESS=true

# Create directory for logs
RUN mkdir -p /app/logs && chmod 777 /app/logs

# Expose the application port
EXPOSE 8082

# Entrypoint to run the application 
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
