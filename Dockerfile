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

# Set timezone
USER root
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

# Create entrypoint script for starting both backend and frontend
USER root
RUN cat > /workspace/entrypoint.sh <<'EOF'
#!/bin/bash
set -e

echo "🚀 Starting mirelplatform..."

# Load NVM environment for Node.js
export NVM_DIR="/home/vscode/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

# Ensure Node.js is in PATH (explicit version for Docker environment)
export PATH="$NVM_DIR/versions/node/v22.20.0/bin:$PATH"

# Verify Node.js is available
if ! command -v node &> /dev/null; then
  echo "❌ Error: node command not found"
  echo "NVM_DIR: $NVM_DIR"
  echo "PATH: $PATH"
  exit 1
fi
echo "Node.js version: $(node --version)"
echo "pnpm version: $(pnpm --version)"

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

# Start Spring Boot on port 3000 (matching EXPOSE directive)
java -jar "$JAR_FILE" \
  --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} \
  --server.port=3000 &
BACKEND_PID=$!
echo "✅ Backend started (PID: $BACKEND_PID) on port 3000"

# Start Frontend in preview mode
echo "🎨 Starting Frontend (Vite Preview)..."
cd /workspace/apps/frontend-v3
# Set backend URL for Vite proxy (defaults to localhost:3000 if not set)
export VITE_BACKEND_URL=${VITE_BACKEND_URL:-http://localhost:3000}
echo "Backend URL: $VITE_BACKEND_URL"
# Use exec to replace the shell process so signals are properly forwarded
exec pnpm preview --host 0.0.0.0 --port 5173
EOF

RUN chmod +x /workspace/entrypoint.sh

# Expose ports
# 3000: Backend API
# 5173: Frontend v3 (Vite)
EXPOSE 3000 5173

# Switch to vscode user
USER vscode

# Health check for backend
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3000/mipla2/actuator/health || exit 1

# Start both backend and frontend via entrypoint script
CMD ["/workspace/entrypoint.sh"]
