# ============================================================================
# Dockerfile for Trade Item Service (Port 8084)
# Multi-stage build: Stage 1 compiles Java with Maven, Stage 2 runs with Alpine JRE
# This service handles trade item processing including GTIN (Global Trade Item Number)
# and supplier management events routed from the loader-service
# ============================================================================

# Stage 1: Build
# Use Maven 3.9 with Eclipse Temurin JDK 17 as the build environment
# Provides both the Maven build tool and Java 17 compiler
# "AS build" creates a named stage reference for copying artifacts into Stage 2
FROM maven:3.9-eclipse-temurin-17 AS build

# Set /app as the working directory for all subsequent build commands
WORKDIR /app

# 1. Copy the Root POM which defines the parent project structure, shared dependencies, and properties
COPY pom.xml .

# 2. Copy ALL module POMs so the Maven Reactor can resolve the full multi-module dependency graph
# Every module POM must be present even though we only build trade-item-service
COPY consumer-service/pom.xml ./consumer-service/
# Copy bff-service POM for reactor dependency resolution
COPY bff-service/pom.xml ./bff-service/
# Copy producer-service POM for reactor resolution
COPY producer-service/pom.xml ./producer-service/
# Copy item-service POM for reactor resolution
COPY item-service/pom.xml ./item-service/
# Copy supplier-supply-service POM for reactor resolution
COPY supplier-supply-service/pom.xml ./supplier-supply-service/
# Copy loader-service POM for reactor resolution
COPY loader-service/pom.xml ./loader-service/
# Copy trade-item-service POM -- this is the target module being built
COPY trade-item-service/pom.xml ./trade-item-service/
# Copy shipment-service POM for reactor resolution
COPY shipment-service/pom.xml ./shipment-service/
# Copy osp-mock-api POM for reactor resolution
COPY osp-mock-api/pom.xml ./osp-mock-api/

# 3. Download and cache all Maven dependencies for the trade-item-service module
# -pl trade-item-service: target only this module
# -am: also make (include) any required parent/upstream modules
# dependency:go-offline: download all dependencies to the local Maven repository cache
# -B: batch mode for non-interactive CI/CD-friendly execution
# This layer is cached by Docker; dependencies are only re-downloaded when POM files change
RUN mvn -pl trade-item-service -am dependency:go-offline -B

# 4. Copy the Java source code for the trade-item-service module only
# Done after dependency caching so source code changes trigger only recompilation, not re-download
COPY trade-item-service/src ./trade-item-service/src

# 5. Build the trade-item-service into an executable Spring Boot JAR
# -pl trade-item-service: compile only this module
# -am: also make required dependency modules
# clean: remove old build artifacts from target/
# package: compile Java source, process Lombok annotations, and create the JAR
# -DskipTests: skip unit tests during Docker build for faster image creation
RUN mvn -pl trade-item-service -am clean package -DskipTests

# Stage 2: Runtime
# Use Eclipse Temurin JRE 17 on Alpine Linux as the minimal production runtime
# JRE-only image excludes the compiler, reducing image size significantly
# Alpine base is ~5MB compared to ~120MB for Debian-based images
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the runtime container
WORKDIR /app

# Copy the compiled Spring Boot fat JAR from the build stage
# --from=build: reference Stage 1's filesystem
# Wildcard *.jar matches the generated executable JAR in the target directory
# Renamed to app.jar for a consistent, predictable entrypoint filename
COPY --from=build /app/trade-item-service/target/*.jar app.jar

# Declare that this container listens on port 8080 at runtime
# Note: trade-item-service runs on port 8084 per application.yml configuration
# This EXPOSE value is documentation only; actual port mapping is in docker-compose.yml (8084:8084)
EXPOSE 8080

# Define the container startup command using exec form (JSON array)
# Exec form ensures java is PID 1, enabling direct signal handling for graceful shutdown
# Starts the Spring Boot application with embedded Tomcat
ENTRYPOINT ["java", "-jar", "app.jar"]
