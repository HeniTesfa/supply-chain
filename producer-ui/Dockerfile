# ============================================================================
# Dockerfile for Producer UI (Port 3000)
# Multi-stage build: Stage 1 builds the React app with Node.js, Stage 2 serves it with Nginx
# This is the frontend interface for creating supply chain events (items, trade items,
# supplier supply, and shipments) which are sent to the producer-service REST API
# ============================================================================

# Multi-stage build for Producer UI

# Stage 1: Build React app
# Use Node.js 18 on Alpine Linux as the build environment for compiling the React application
# Alpine variant keeps the build image small while providing full Node.js capabilities
# "AS build" names this stage so we can reference its filesystem when copying build output
FROM node:18-alpine AS build

# Set /app as the working directory inside the build container
# All subsequent COPY and RUN commands execute relative to this directory
WORKDIR /app

# Copy package.json and package-lock.json from the producer-ui directory
# The wildcard package*.json matches both files
# These are copied first (before source code) to leverage Docker layer caching:
# npm install only re-runs when package files change, not on every source code edit
COPY producer-ui/package*.json ./

# Install all Node.js dependencies defined in package.json
# This downloads React, Axios, and all other npm packages into node_modules/
# The dependencies are cached in this Docker layer for faster subsequent builds
RUN npm install

# Copy the entire producer-ui source directory into the container
# This includes src/ (React components, CSS), public/ (index.html, assets), and config files
# Done after npm install so source code changes don't trigger a full dependency reinstall
COPY producer-ui/ .

# Build the React application for production deployment
# This runs react-scripts build which creates an optimized production bundle in the build/ directory
# The output includes minified JavaScript, CSS, and static assets with content hashes for caching
RUN npm run build

# Stage 2: Serve with Nginx
# Use the official Nginx image on Alpine Linux as the production web server
# Nginx efficiently serves static files and handles reverse proxying to the backend API
# Alpine variant keeps the final image very small (~25MB)
FROM nginx:alpine

# Set the working directory to Nginx's default document root
# This is where Nginx looks for static files to serve
WORKDIR /usr/share/nginx/html

# Remove the default Nginx welcome page and sample content
# This clears the directory so only our React build output is served
# The ./* glob removes all files and directories in the current working directory
RUN rm -rf ./*

# Copy the production-built React application from Stage 1 into Nginx's document root
# --from=build: reference the build stage's filesystem
# The build/ directory contains the optimized React app (index.html, JS bundles, CSS, assets)
COPY --from=build /app/build .

# Copy the custom Nginx configuration file into the Nginx config directory
# This config file sets up: port 3000 listening, SPA routing (try_files), and
# reverse proxy for /api/ requests to the producer-service backend (port 8087)
# Replaces the default Nginx server block configuration
COPY producer-ui/nginx.conf /etc/nginx/conf.d/default.conf

# Declare that this container listens on port 3000 at runtime
# This matches the listen directive in the custom nginx.conf
# EXPOSE is documentation only; actual port publishing is done in docker-compose.yml
EXPOSE 3000

# Start Nginx in the foreground (not as a daemon)
# "daemon off" keeps Nginx running as PID 1 in the container
# This is required for Docker -- if Nginx daemonizes, the container would exit immediately
# Docker monitors PID 1 to determine if the container is still running
CMD ["nginx", "-g", "daemon off;"]
