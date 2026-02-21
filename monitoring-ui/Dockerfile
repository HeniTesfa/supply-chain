# ============================================================================
# Dockerfile for Monitoring UI (Port 3001)
# Multi-stage build: Stage 1 builds the React app with Node.js, Stage 2 serves it with Nginx
# This is the frontend dashboard for monitoring supply chain processing errors
# It displays error statistics, unresolved errors, and error stage information
# (DB_SAVE, OSP_API, LOADER_SERVICE failures) fetched from the consumer-service API
# ============================================================================

# Multi-stage build for Monitoring UI

# Stage 1: Build React app
# Use Node.js 18 on Alpine Linux as the build environment for the React application
# Alpine variant provides a smaller base image while including full Node.js tooling
# "AS build" names this stage for referencing its filesystem when copying build artifacts
FROM node:18-alpine AS build

# Set /app as the working directory inside the build container
# All subsequent COPY and RUN commands operate relative to this path
WORKDIR /app

# Copy package.json and package-lock.json from the monitoring-ui directory
# The wildcard package*.json matches both files
# Copied before source code to leverage Docker layer caching:
# npm install will only re-run when these dependency declaration files change
COPY monitoring-ui/package*.json ./

# Install all Node.js dependencies defined in package.json
# Downloads React, Axios, Recharts (charting library), and other npm packages
# The installed node_modules/ are cached in this Docker layer for faster rebuilds
RUN npm install

# Copy the entire monitoring-ui source directory into the container
# This includes src/ (Dashboard.js, CSS), public/ (index.html), and config files
# Done after npm install so that source code changes don't invalidate the dependency cache
COPY monitoring-ui/ .

# Build the React application for production
# Runs react-scripts build to create an optimized bundle in the build/ directory
# Output includes minified JS, CSS, and static assets with content hashes for browser caching
RUN npm run build

# Stage 2: Serve with Nginx
# Use the official Nginx image on Alpine Linux as the production static file server
# Nginx efficiently serves the React build output and proxies API requests to the backend
# Alpine variant keeps the final production image very small (~25MB)
FROM nginx:alpine

# Set the working directory to Nginx's default document root
# Nginx serves static files from this directory by default
WORKDIR /usr/share/nginx/html

# Remove the default Nginx welcome page and sample content
# Clears the document root so only our React build output will be served
# The ./* glob matches all files and subdirectories in the current directory
RUN rm -rf ./*

# Copy the production-built React application from Stage 1 into the Nginx document root
# --from=build: reference the build stage's filesystem
# The build/ directory contains the compiled React app (index.html, JS bundles, CSS)
COPY --from=build /app/build .

# Copy the custom Nginx configuration file that configures:
# - Listening on port 3001 (monitoring UI port)
# - SPA routing with try_files for React Router support
# - Reverse proxy for /api/ requests to consumer-service (port 8081) for error data
# This replaces the default Nginx server block configuration
COPY monitoring-ui/nginx.conf /etc/nginx/conf.d/default.conf

# Declare that this container listens on port 3001 at runtime
# This matches the listen directive in the custom nginx.conf
# EXPOSE is documentation only; actual port publishing is handled in docker-compose.yml
EXPOSE 3001

# Start Nginx in the foreground (not as a daemon)
# "daemon off" directive keeps Nginx as PID 1 in the container
# This is essential for Docker -- without it, Nginx would fork to background and the container exits
# Docker monitors PID 1 to determine container lifecycle (running vs. stopped)
CMD ["nginx", "-g", "daemon off;"]
