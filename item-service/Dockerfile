# ============================================================================
# Dockerfile for Item Service (Port 8083)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service handles item event processing and integrates with the external OSP API
# It implements exponential backoff retry logic (2^n * 1000ms) for OSP API calls
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# This base image includes both the Maven build tool and Java 17 JDK
# "AS build" names this stage so we can reference its filesystem later
FROM maven:3.9-eclipse-temurin-17 AS build

# Set /app as the working directory for all build commands
WORKDIR /app

# 1. Copy the Root POM which defines shared properties, dependency versions, and module declarations
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the complete multi-module project
# All module POMs must be present for Maven to understand the full dependency tree
COPY consumer-service/pom.xml ./consumer-service/
# Copy bff-service POM for reactor dependency resolution
COPY bff-service/pom.xml ./bff-service/
# Copy producer-service POM for reactor dependency graph resolution
COPY producer-service/pom.xml ./producer-service/
# Copy item-service POM -- this is the module we are building
COPY item-service/pom.xml ./item-service/
# Copy supplier-supply-service POM for reactor dependency graph resolution
COPY supplier-supply-service/pom.xml ./supplier-supply-service/
# Copy loader-service POM for reactor dependency graph resolution
COPY loader-service/pom.xml ./loader-service/
# Copy trade-item-service POM for reactor dependency graph resolution
COPY trade-item-service/pom.xml ./trade-item-service/
# Copy shipment-service POM for reactor dependency graph resolution
COPY shipment-service/pom.xml ./shipment-service/
# Copy osp-mock-api POM for reactor dependency graph resolution
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Download and cache all Maven dependencies for item-service
# -pl item-service: target only the item-service module
# -am: also make any modules item-service depends on (parent POM, etc.)
# dependency:go-offline: download all required dependencies to local Maven cache
# -B: batch mode for non-interactive execution (no progress bar output)
# This Docker layer is cached and only re-runs when POM files change
RUN mvn -pl item-service -am dependency:go-offline -B

# 4. Copy the Java source code for the item-service module
# Placed after dependency download so source code changes don't bust the dependency cache
COPY item-service/src ./item-service/src

# 5. Build the item-service module into a Spring Boot executable JAR
# -pl item-service: compile only the item-service module
# -am: also make any required upstream modules
# clean: remove previous build artifacts from target/
# package: compile, run annotation processors (Lombok), and create the JAR
# -DskipTests: skip test execution for faster Docker builds
RUN mvn -pl item-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux for a minimal production image
# Alpine-based JRE images are very small, reducing download time and attack surface
# Note: Unlike some other services, this Dockerfile does not install libc6-compat
# This means Snappy compression may not work; item-service may not use Kafka directly
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the runtime container
WORKDIR /app

# Copy the compiled fat JAR from the build stage into the runtime image
# --from=build: pull from Stage 1's filesystem
# Wildcard *.jar matches the single Spring Boot JAR in the target directory
# Renamed to app.jar for a consistent entrypoint filename
COPY --from=build /app/item-service/target/*.jar app.jar

# Declare that this container listens on port 8080
# Note: item-service runs on port 8083 in application.yml
# This EXPOSE value does not match the actual port; it serves as documentation only
# Port mapping in docker-compose.yml (8083:8083) determines the actual published port
EXPOSE 8080

# Define the container's startup command using exec form (JSON array)
# Exec form ensures java is PID 1 and receives OS signals (SIGTERM) for graceful shutdown
# Starts the Spring Boot application with its embedded Tomcat server
ENTRYPOINT ["java", "-jar", "app.jar"]
