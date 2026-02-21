# ============================================================================
# Dockerfile for Producer Service (Port 8087)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service provides REST APIs for event creation and publishes events to Kafka topics
# It also implements producer-side deduplication using idempotency keys and content hashes
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# This image provides both Maven build tool and Java 17 compiler
# The "AS build" alias allows referencing this stage's filesystem in Stage 2
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory inside the build container to /app
# All subsequent COPY and RUN commands will execute relative to this directory
WORKDIR /app

# 1. Copy the Root POM (parent pom.xml) into the container
# The root POM defines the multi-module project structure, shared properties, and dependency versions
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the complete project dependency graph
# Maven requires visibility into all modules even when building a single one
# This enables proper dependency resolution across the multi-module project
COPY consumer-service/pom.xml ./consumer-service/
# Copy bff-service POM for reactor dependency resolution
COPY bff-service/pom.xml ./bff-service/
# Copy producer-service POM -- this is the module we are actually building
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

# 3. Cache dependencies for the producer-service module
# -pl producer-service: only process the producer-service module
# -am: also make (build) required parent/dependency modules
# dependency:go-offline: download all required JARs to the local Maven repository cache
# -B: batch mode -- suppresses interactive prompts and download progress output
# Docker caches this layer; dependencies are only re-downloaded when pom.xml files change
RUN mvn -pl producer-service -am dependency:go-offline -B

# 4. Copy the actual Java source code for the producer-service module
# Done AFTER dependency caching so that source code edits don't trigger a full dependency re-download
COPY producer-service/src ./producer-service/src

# 5. Build the producer-service module into an executable Spring Boot JAR
# -pl producer-service: build only the producer-service module
# -am: also make any modules it depends on
# clean package: remove old artifacts, then compile, test, and package the application
# -DskipTests: skip unit tests during Docker builds for faster image creation
RUN mvn -pl producer-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux for a minimal production runtime
# Alpine-based images are significantly smaller, reducing deployment size and attack surface
# JRE is sufficient since the application only needs to run, not compile Java code
FROM eclipse-temurin:17-jre-alpine

# Add glibc compatibility layer needed by Kafka's Snappy compression native libraries
# Alpine uses musl libc, but Snappy's JNI bindings require glibc compatibility
# --no-cache: skip caching the APK package index to keep image size minimal
RUN apk add --no-cache libc6-compat

# Set the working directory for the runtime container to /app
WORKDIR /app

# Copy the compiled JAR file from the build stage into the runtime image
# --from=build: pull the file from the first stage's filesystem
# Wildcard *.jar matches the generated Spring Boot executable JAR
# Renamed to app.jar for a consistent entrypoint reference
COPY --from=build /app/producer-service/target/*.jar app.jar

# Declare that this container listens on port 8082
# Note: The producer-service actually runs on port 8087 in application.yml
# This EXPOSE value (8082) appears to be a copy oversight but does not affect functionality
# EXPOSE is purely documentation -- actual port mapping is done in docker-compose.yml
EXPOSE 8082

# Define the startup command using exec form (JSON array)
# Exec form ensures the Java process is PID 1 and receives OS signals (SIGTERM) directly
# This enables graceful shutdown of the Spring Boot application in Docker/Kubernetes
ENTRYPOINT ["java", "-jar", "app.jar"]
