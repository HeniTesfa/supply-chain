# ============================================================================
# Dockerfile for Consumer Service (Port 8081)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service consumes Kafka events with 4-level deduplication and forwards to loader-service
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# This image includes both Maven and Java 17 needed to compile the Spring Boot application
# The "AS build" alias lets us reference this stage later when copying the built JAR
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory inside the build container to /app
# All subsequent COPY and RUN commands will execute relative to this path
WORKDIR /app

# 1. Copy the Root POM (parent pom.xml) into the container
# The root POM defines shared dependencies, plugins, and module structure for all 8 services
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the multi-module project structure
# Maven needs to see every module's pom.xml to understand inter-module dependencies
# Even though we only build consumer-service, Maven's reactor requires the full project graph
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
# Copy osp-mock-api POM for reactor dependency resolution
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Cache dependencies for the consumer-service module
# -pl consumer-service: only process the consumer-service module
# -am: also make (build) any modules that consumer-service depends on
# dependency:go-offline: download all dependencies to local Maven cache
# -B: run in batch (non-interactive) mode, suppresses download progress output
# This layer is cached by Docker -- dependencies are only re-downloaded when pom.xml files change
RUN mvn -pl consumer-service -am dependency:go-offline -B

# 4. Copy the actual Java source code for the consumer-service module
# This is done AFTER dependency caching so source code changes don't invalidate the dependency cache
COPY consumer-service/src ./consumer-service/src

# 5. Build the consumer-service module into an executable JAR
# -pl consumer-service: target only the consumer-service module
# -am: also make any required parent/dependency modules
# clean: remove any previous build artifacts
# package: compile source, run tests, and package into a JAR file
# -DskipTests: skip running unit tests during Docker build for faster image creation
RUN mvn -pl consumer-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux as the minimal runtime environment
# Alpine-based image is much smaller (~80MB vs ~400MB) than full JDK images
# JRE (not JDK) is used since we only need to RUN Java, not compile it
FROM eclipse-temurin:17-jre-alpine

# Add the glibc compatibility layer required by Kafka's Snappy compression library
# Alpine Linux uses musl libc by default, but Snappy native binaries need glibc
# --no-cache: don't store the package index locally, keeping the image small
RUN apk add --no-cache libc6-compat

# Set the working directory for the runtime container
WORKDIR /app

# Copy the built JAR file from the build stage into the runtime container
# --from=build: reference the first stage (aliased as "build")
# The wildcard *.jar matches the generated Spring Boot fat JAR in the target directory
# Rename it to app.jar for a consistent, predictable entrypoint filename
COPY --from=build /app/consumer-service/target/*.jar app.jar

# Declare that this container listens on port 8081 at runtime
# This is the port configured in consumer-service's application.yml (server.port=8081)
# EXPOSE is documentation only -- the port must still be published with -p or in docker-compose
EXPOSE 8081

# Define the command to run when the container starts
# Uses exec form (JSON array) so the Java process receives OS signals directly (for graceful shutdown)
# Runs the Spring Boot fat JAR which contains an embedded Tomcat server
ENTRYPOINT ["java", "-jar", "app.jar"]
