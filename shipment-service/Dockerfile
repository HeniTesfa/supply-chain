# ============================================================================
# Dockerfile for Shipment Service (Port 8086)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service handles shipment tracking events routed from the loader-service
# It processes tracking numbers, carrier info, and shipment status updates
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# This base image provides both the Maven build tool and Java 17 JDK for compilation
# "AS build" creates a named stage reference for copying the JAR artifact in Stage 2
FROM maven:3.9-eclipse-temurin-17 AS build

# Set /app as the working directory inside the build container
WORKDIR /app

# 1. Copy the Root POM which defines shared dependency management and the multi-module structure
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the full multi-module dependency graph
# Even when building a single module, Maven's reactor needs to see all sibling module POMs
COPY consumer-service/pom.xml ./consumer-service/
# Copy bff-service POM for reactor dependency resolution
COPY bff-service/pom.xml ./bff-service/
# Copy producer-service POM for reactor dependency resolution
COPY producer-service/pom.xml ./producer-service/
# Copy item-service POM for reactor dependency resolution
COPY item-service/pom.xml ./item-service/
# Copy supplier-supply-service POM for reactor dependency resolution
COPY supplier-supply-service/pom.xml ./supplier-supply-service/
# Copy loader-service POM for reactor dependency resolution
COPY loader-service/pom.xml ./loader-service/
# Copy trade-item-service POM for reactor dependency resolution
COPY trade-item-service/pom.xml ./trade-item-service/
# Copy shipment-service POM -- this is the module we are building
COPY shipment-service/pom.xml ./shipment-service/
# Copy osp-mock-api POM for reactor dependency resolution
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Download and cache all Maven dependencies for the shipment-service module
# -pl shipment-service: target only this module
# -am: also make (include) any required parent or dependency modules
# dependency:go-offline: download all JARs and plugins to the local Maven repository
# -B: batch mode for non-interactive Docker/CI execution
# Docker caches this layer so dependencies are only re-downloaded when POM files change
RUN mvn -pl shipment-service -am dependency:go-offline -B

# 4. Copy the Java source code for the shipment-service module
# Placed after dependency caching so source code edits don't invalidate the dependency layer
COPY shipment-service/src ./shipment-service/src

# 5. Compile and package the shipment-service into an executable Spring Boot JAR
# -pl shipment-service: build only this specific module
# -am: also make any required parent/upstream modules
# clean: remove old build output from the target/ directory
# package: compile source code, process annotations, and package into a JAR
# -DskipTests: skip running unit tests during the Docker build for faster image creation
RUN mvn -pl shipment-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux for a small, production-ready runtime image
# Alpine-based image greatly reduces the final image size compared to Debian-based alternatives
# JRE-only image excludes compilation tools since we only need to execute the application
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the runtime container
WORKDIR /app

# Copy the compiled Spring Boot fat JAR from the build stage into the runtime image
# --from=build: pull from Stage 1's filesystem
# Wildcard *.jar matches the generated executable JAR in the target directory
# Renamed to app.jar for a simple, consistent entrypoint filename
COPY --from=build /app/shipment-service/target/*.jar app.jar

# Declare that this container listens on port 8080 at runtime
# Note: shipment-service runs on port 8086 per application.yml configuration
# EXPOSE is documentation only; actual port mapping is configured in docker-compose.yml (8086:8086)
EXPOSE 8080

# Define the container startup command using exec form (JSON array)
# Exec form ensures java is PID 1 and receives OS signals directly for graceful shutdown
# Starts the Spring Boot application with its embedded Tomcat server
ENTRYPOINT ["java", "-jar", "app.jar"]
