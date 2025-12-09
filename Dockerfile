# Multi-stage Dockerfile for mirelplatform
# IT Operations Environment - Backend + Frontend development
# Based on .devcontainer/devcontainer.json configuration
# 
# BuildKit cache mounts are used to optimize build performance:
# - Gradle: /home/vscode/.gradle
# - npm/pnpm: /home/vscode/.npm, /home/vscode/.cache/pnpm
#
# Enable BuildKit: DOCKER_BUILDKIT=1 docker build ...

# Stage 1: Base image with Java 21 and Node.js 22
FROM mcr.microsoft.com/devcontainers/java:21 AS base

# Set default shell to bash for nvm compatibility
SHELL ["/bin/bash", "-c"]

# Install Node.js 22 using nvm
ARG NODE_VERSION=22
ARG NVM_VERSION=0.39.7

# Install dependencies
USER root
RUN apt-get update && apt-get install -y git git-lfs curl ca-certificates && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install nvm and Node.js as vscode user
USER vscode
ENV NVM_DIR=/home/vscode/.nvm
RUN mkdir -p ${NVM_DIR} \
    && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v${NVM_VERSION}/install.sh | bash \
    && . "${NVM_DIR}/nvm.sh" \
    && nvm install ${NODE_VERSION} \
    && nvm use ${NODE_VERSION} \
    && nvm alias default ${NODE_VERSION}

# Add Node.js to PATH
ENV NODE_PATH=${NVM_DIR}/versions/node/v${NODE_VERSION}/lib/node_modules
ENV PATH=${NVM_DIR}/versions/node/v${NODE_VERSION}/bin:$PATH

# Stage 2: Dependencies and build
FROM base AS builder

# Re-declare ARG for this stage
ARG NODE_VERSION=22

# Set environment variables
ENV GRADLE_USER_HOME=/home/vscode/.gradle \
    NVM_DIR=/home/vscode/.nvm \
    TZ=Asia/Tokyo \
    SPRING_PROFILES_ACTIVE=dev \
    HOST=0.0.0.0
ENV PATH=${NVM_DIR}/versions/node/v${NODE_VERSION}/bin:$PATH

# Set working directory
WORKDIR /workspace

# Copy gradle wrapper and build files first (for caching)
COPY --chown=vscode:vscode gradle gradle
COPY --chown=vscode:vscode gradlew gradlew.bat gradle.properties settings.gradle build.gradle ./
COPY --chown=vscode:vscode lombok.config ./

# Copy backend build configuration
COPY --chown=vscode:vscode backend/build.gradle backend/

# Download dependencies with cache mount
USER vscode
RUN --mount=type=cache,target=/home/vscode/.gradle,uid=1000,gid=1000 \
    ./gradlew --no-daemon dependencies

# Copy package.json files for npm dependencies (for caching)
COPY --chown=vscode:vscode package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY --chown=vscode:vscode apps/frontend-v3/package.json apps/frontend-v3/
COPY --chown=vscode:vscode packages/ui/package.json packages/ui/

# Install pnpm and dependencies with cache mount
RUN --mount=type=cache,target=/home/vscode/.npm,uid=1000,gid=1000 \
    --mount=type=cache,target=/home/vscode/.cache/pnpm,uid=1000,gid=1000 \
    . "${NVM_DIR}/nvm.sh" \
    && npm install -g pnpm@latest \
    && pnpm install --frozen-lockfile

# Copy all source code
COPY --chown=vscode:vscode . .

# Build backend (skip tests for faster build) with cache mount
RUN --mount=type=cache,target=/home/vscode/.gradle,uid=1000,gid=1000 \
    ./gradlew --no-daemon build -x test

# Build frontend for production
RUN . "${NVM_DIR}/nvm.sh" \
    && pnpm --filter frontend-v3 build

# Stage 3: Runtime image
FROM base AS runtime

# Re-declare ARG for this stage
ARG NODE_VERSION=22

# Set environment variables
ENV GRADLE_USER_HOME=/home/vscode/.gradle \
    NVM_DIR=/home/vscode/.nvm \
    TZ=Asia/Tokyo \
    SPRING_PROFILES_ACTIVE=dev \
    HOST=0.0.0.0
ENV PATH=${NVM_DIR}/versions/node/v${NODE_VERSION}/bin:$PATH

# Install Nginx for serving frontend static files
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    nginx \
    && rm -rf /var/lib/apt/lists/*

# Set timezone
RUN ln -sf /usr/share/zoneinfo/Asia/Tokyo /etc/localtime \
    && echo 'Asia/Tokyo' > /etc/timezone

# Set working directory
WORKDIR /workspace

# Copy built artifacts and dependencies from builder stage
# Note: Cache mounts (/home/vscode/.gradle, /home/vscode/.npm) are not persisted in the image
# Only copy the workspace which contains the built artifacts
COPY --from=builder --chown=vscode:vscode /home/vscode/.nvm /home/vscode/.nvm
COPY --from=builder --chown=vscode:vscode /workspace /workspace

# Create logs directory for Spring Boot (Logback configuration)
RUN mkdir -p /workspace/logs && chown -R vscode:vscode /workspace/logs

# Create Nginx configuration for SPA and API proxy
RUN cat > /etc/nginx/sites-available/mirelplatform <<'EOF'
server {
    listen 5173;
    server_name _;

    root /var/www/html;
    index index.html;

    # Enable gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;

    # API proxy to Backend (Spring Boot on port 3000)
    location /mapi/ {
        proxy_pass http://localhost:3000/mipla2/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    # Static files with cache headers
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # SPA fallback - serve index.html for all routes
    location / {
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "no-cache";
    }
}
EOF

RUN ln -sf /etc/nginx/sites-available/mirelplatform /etc/nginx/sites-enabled/ \
    && rm -f /etc/nginx/sites-enabled/default

# Copy built frontend to Nginx document root
RUN mkdir -p /var/www/html \
    && cp -r /workspace/apps/frontend-v3/dist/* /var/www/html/ \
    && chown -R www-data:www-data /var/www/html

# Create entrypoint script for starting backend and nginx
USER root
RUN cat > /workspace/entrypoint.sh <<'EOF'
#!/bin/bash
set -e

echo "🚀 Starting mirelplatform..."

# Start Backend in background
echo "📦 Starting Backend (Spring Boot)..."
cd /workspace
# Find JAR file (exclude plain.jar which is not executable)
JAR_FILE=$(find backend/build/libs -name "*.jar" ! -name "*plain.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
  echo "❌ Error: No JAR file found in backend/build/libs/"
  exit 1
fi
echo "Found JAR: $JAR_FILE"

# Start Spring Boot on port 3000
java -jar "$JAR_FILE" \
  --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} \
  --server.port=3000 &
BACKEND_PID=$!
echo "✅ Backend started (PID: $BACKEND_PID) on port 3000"

# Start Nginx in foreground
echo "🌐 Starting Nginx (Frontend on port 5173)..."
# Use exec to replace the shell process so signals are properly forwarded
exec nginx -g 'daemon off;'
EOF

RUN chmod +x /workspace/entrypoint.sh

# Expose ports
# 3000: Backend API (internal, proxied through Nginx)
# 5173: Frontend (Nginx)
EXPOSE 3000 5173

# Nginx requires root to bind to ports
USER root

# Health check for backend
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3000/mipla2/actuator/health || exit 1

# Start both backend and nginx via entrypoint script
CMD ["/workspace/entrypoint.sh"]
