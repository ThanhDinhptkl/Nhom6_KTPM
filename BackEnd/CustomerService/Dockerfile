# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Set default environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Application Configuration
ENV SPRING_APPLICATION_NAME=CustomerService
ENV SERVER_PORT=8081

# Database Configuration
ENV SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/tour
ENV SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver

# Hibernate Configuration
ENV SPRING_JPA_SHOW_SQL=true
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

# Eureka Client Configuration - Temporarily disabled
# ENV EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://discovery-server:8761/eureka/
ENV EUREKA_CLIENT_ENABLED=false

# Create directory for logs
RUN mkdir -p /app/logs && chmod 777 /app/logs

# Expose the application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
