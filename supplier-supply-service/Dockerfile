# ============================================================================
# Dockerfile for Supplier Supply Service (Port 8085)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service handles supplier-level supply tracking events routed from the loader-service
# It processes supply quantity, unit cost, and supplier information
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# Provides both Maven and the Java 17 JDK needed for compilation
# "AS build" names this stage for referencing when copying the JAR in Stage 2
FROM maven:3.9-eclipse-temurin-17 AS build

# Set /app as the working directory for all build operations
WORKDIR /app

# 1. Copy the Root POM defining the parent project structure and shared dependency management
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can see the entire multi-module project structure
# Maven requires all module POMs to properly resolve inter-module dependencies
COPY consumer-service/pom.xml ./consumer-service/
# Copy bff-service POM for reactor dependency resolution
COPY bff-service/pom.xml ./bff-service/
# Copy producer-service POM for reactor dependency resolution
COPY producer-service/pom.xml ./producer-service/
# Copy item-service POM for reactor dependency resolution
COPY item-service/pom.xml ./item-service/
# Copy supplier-supply-service POM -- this is the module being built
COPY supplier-supply-service/pom.xml ./supplier-supply-service/
# Copy loader-service POM for reactor dependency resolution
COPY loader-service/pom.xml ./loader-service/
# Copy trade-item-service POM for reactor dependency resolution
COPY trade-item-service/pom.xml ./trade-item-service/
# Copy shipment-service POM for reactor dependency resolution
COPY shipment-service/pom.xml ./shipment-service/
# Copy osp-mock-api POM for reactor dependency resolution
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Download and cache all Maven dependencies for the supplier-supply-service module
# -pl supplier-supply-service: target only this module
# -am: also make (include) parent and dependency modules
# dependency:go-offline: resolve and download all required JARs to local Maven cache
# -B: batch mode for non-interactive Docker/CI execution
# Docker layer caching ensures this step only re-runs when POM files change
RUN mvn -pl supplier-supply-service -am dependency:go-offline -B

# 4. Copy the Java source code for the supplier-supply-service module
# Placed after dependency caching to maximize Docker layer cache efficiency
COPY supplier-supply-service/src ./supplier-supply-service/src

# 5. Compile and package the supplier-supply-service into an executable Spring Boot JAR
# -pl supplier-supply-service: build only this module
# -am: also make any required upstream modules
# clean: remove previous build output from target/
# package: compile, process annotations (Lombok), and create the executable JAR
# -DskipTests: skip test execution to speed up Docker builds
RUN mvn -pl supplier-supply-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux for minimal production footprint
# Alpine-based JRE images are very lightweight, reducing storage and startup time
# JRE is sufficient since we only need to run the compiled application
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the runtime container
WORKDIR /app

# Copy the compiled fat JAR from the build stage into the runtime container
# --from=build: reference Stage 1's filesystem
# Wildcard *.jar matches the generated Spring Boot executable JAR
# Renamed to app.jar for a consistent entrypoint reference
COPY --from=build /app/supplier-supply-service/target/*.jar app.jar

# Declare that this container listens on port 8080 at runtime
# Note: supplier-supply-service runs on port 8085 per application.yml
# EXPOSE is documentation only; actual port publishing is configured in docker-compose.yml (8085:8085)
EXPOSE 8080

# Define the container startup command using exec form (JSON array)
# Exec form ensures java is PID 1, receiving OS signals (SIGTERM) for graceful shutdown
# Launches the Spring Boot application with its embedded Tomcat server
ENTRYPOINT ["java", "-jar", "app.jar"]
