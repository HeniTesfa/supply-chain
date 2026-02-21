# ============================================================================
# Dockerfile for Loader Service (Port 8082)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service acts as an event router, receiving events from consumer-service
# and forwarding them to the appropriate downstream service (item, trade-item,
# supplier-supply, or shipment) based on the event type or field-presence inference
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# Provides both Maven and Java 17 needed for compiling the Spring Boot application
# "AS build" creates a named reference to copy artifacts from this stage later
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory inside the container to /app
# All subsequent COPY and RUN instructions operate relative to this path
WORKDIR /app

# 1. Copy the Root POM (parent pom.xml) which defines shared dependencies and module structure
COPY pom.xml .

# 2. Copy ALL module POMs so Maven's Reactor can resolve the full multi-module dependency graph
# Maven needs to see every module POM even when building just one module
COPY consumer-service/pom.xml ./consumer-service/
# Copy bff-service POM for reactor dependency resolution
COPY bff-service/pom.xml ./bff-service/
# Copy producer-service POM for reactor resolution
COPY producer-service/pom.xml ./producer-service/
# Copy item-service POM for reactor resolution
COPY item-service/pom.xml ./item-service/
# Copy supplier-supply-service POM for reactor resolution
COPY supplier-supply-service/pom.xml ./supplier-supply-service/
# Copy loader-service POM -- this is the target module being built
COPY loader-service/pom.xml ./loader-service/
# Copy trade-item-service POM for reactor resolution
COPY trade-item-service/pom.xml ./trade-item-service/
# Copy shipment-service POM for reactor resolution
COPY shipment-service/pom.xml ./shipment-service/
# Copy osp-mock-api POM for reactor resolution
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Download and cache all Maven dependencies for the loader-service module
# -pl loader-service: target only the loader-service module
# -am: also make (include) any modules that loader-service depends on
# dependency:go-offline: resolve and download all dependencies into local cache
# -B: batch mode, suppresses interactive prompts and download progress logging
# Docker caches this layer -- dependencies won't re-download unless pom.xml files change
RUN mvn -pl loader-service -am dependency:go-offline -B

# 4. Copy the Java source code for the loader-service module only
# Source code is copied AFTER dependencies so code changes don't invalidate the dependency cache
COPY loader-service/src ./loader-service/src

# 5. Compile and package the loader-service into an executable Spring Boot JAR
# -pl loader-service: build only the loader-service module
# -am: include required parent/dependency modules in the build
# clean: delete previous build output from the target directory
# package: compile, run tests, and create the executable JAR
# -DskipTests: skip unit test execution to speed up the Docker build process
RUN mvn -pl loader-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux as the lightweight production runtime
# Alpine is much smaller than Debian-based images (~5MB base vs ~120MB)
# JRE-only image excludes the compiler toolchain since we only need to run the app
FROM eclipse-temurin:17-jre-alpine

# Install glibc compatibility layer needed by Kafka/Snappy native compression libraries
# Alpine Linux uses musl libc; the Snappy JNI bindings require glibc compatibility
# --no-cache prevents storing the APK index locally, keeping the final image smaller
RUN apk add --no-cache libc6-compat

# Set the working directory for the runtime container
WORKDIR /app

# Copy the built JAR artifact from the build stage into the runtime image
# --from=build: reference the Stage 1 filesystem
# Wildcard *.jar matches the Spring Boot fat JAR generated in the target/ directory
# Renamed to app.jar for a simple, consistent entrypoint reference
COPY --from=build /app/loader-service/target/*.jar app.jar

# Declare that this container listens on port 8080
# Note: The loader-service runs on port 8082 per application.yml configuration
# This EXPOSE value may not match the actual server port; it's documentation only
# Actual port mapping is handled by docker-compose.yml (8082:8082)
EXPOSE 8080

# Define the container startup command using exec form (JSON array)
# Exec form makes the java process PID 1, so it receives SIGTERM directly for graceful shutdown
# Launches the Spring Boot embedded Tomcat server from the fat JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
