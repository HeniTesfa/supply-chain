# ============================================================================
# Dockerfile for BFF Service (Port 8080)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service acts as the Backend-for-Frontend API Gateway, proxying requests
# from React frontends to producer-service and consumer-service
# ============================================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 1. Copy the Root POM (parent pom.xml)
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the multi-module project structure
COPY bff-service/pom.xml ./bff-service/
COPY consumer-service/pom.xml ./consumer-service/
COPY producer-service/pom.xml ./producer-service/
COPY item-service/pom.xml ./item-service/
COPY supplier-supply-service/pom.xml ./supplier-supply-service/
COPY loader-service/pom.xml ./loader-service/
COPY trade-item-service/pom.xml ./trade-item-service/
COPY shipment-service/pom.xml ./shipment-service/
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Cache dependencies
RUN mvn -pl bff-service -am dependency:go-offline -B

# 4. Copy the actual Java source code
COPY bff-service/src ./bff-service/src

# 5. Build the bff-service module
RUN mvn -pl bff-service -am clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/bff-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
