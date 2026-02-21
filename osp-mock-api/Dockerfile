# ============================================================================
# Dockerfile for OSP Mock API (Port 9000)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service is a mock implementation of the external OSP (Order Service Provider) API
# It simulates the real OSP API responses for development and testing purposes
# The item-service calls this API with exponential backoff retry logic
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# Provides both the Maven build tool and Java 17 JDK for compilation
# "AS build" names this stage for referencing its filesystem in Stage 2
FROM maven:3.9-eclipse-temurin-17 AS build

# Set /app as the working directory for all build operations inside the container
WORKDIR /app

# 1. Copy the Root POM which defines shared properties, dependency management, and module list
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the complete multi-module structure
# Maven requires all module POMs to build the dependency graph, even for a single-module build
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
# Copy shipment-service POM for reactor dependency resolution
COPY shipment-service/pom.xml ./shipment-service/
# Copy osp-mock-api POM -- this is the module we are building
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Download and cache all Maven dependencies for the osp-mock-api module
# -pl osp-mock-api: target only this module
# -am: also make (include) any required parent/dependency modules
# dependency:go-offline: download all required JARs and plugins to local Maven cache
# -B: batch mode for non-interactive Docker/CI-friendly output
# Docker layer caching means this only re-runs when POM files are modified
RUN mvn -pl osp-mock-api -am dependency:go-offline -B

# 4. Copy the Java source code for the osp-mock-api module
# Placed after dependency caching for optimal Docker layer cache efficiency
COPY osp-mock-api/src ./osp-mock-api/src

# 5. Compile and package the osp-mock-api module into an executable Spring Boot JAR
# -pl osp-mock-api: build only this module
# -am: also make required parent/upstream modules
# clean: remove previous build artifacts from target/
# package: compile Java source, process annotations, and create executable JAR
# -DskipTests: skip unit test execution during Docker build for faster builds
RUN mvn -pl osp-mock-api -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux for a lightweight production runtime
# Alpine-based images reduce final image size and download time
# JRE is sufficient since we only need to run the compiled Java application
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the runtime container
WORKDIR /app

# Copy the compiled Spring Boot fat JAR from the build stage into the runtime image
# --from=build: reference Stage 1's filesystem
# Wildcard *.jar matches the generated executable JAR in the target directory
# Renamed to app.jar for a consistent, predictable entrypoint reference
COPY --from=build /app/osp-mock-api/target/*.jar app.jar

# Declare that this container listens on port 8080 at runtime
# Note: osp-mock-api runs on port 9000 per application.yml configuration
# EXPOSE is documentation only; actual port mapping is in docker-compose.yml (9000:9000)
EXPOSE 8080

# Define the container startup command using exec form (JSON array)
# Exec form ensures java is PID 1 and receives SIGTERM directly for graceful shutdown
# Starts the Spring Boot mock API application with its embedded Tomcat server
ENTRYPOINT ["java", "-jar", "app.jar"]
