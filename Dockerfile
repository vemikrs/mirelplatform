# Multi-stage Dockerfile for mirelplatform
# Based on .devcontainer/devcontainer.json configuration

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

# Set environment variables
ENV GRADLE_USER_HOME=/home/vscode/.gradle \
    NVM_DIR=/home/vscode/.nvm \
    TZ=Asia/Tokyo \
    SPRING_PROFILES_ACTIVE=dev \
    HOST=0.0.0.0 \
    PLAYWRIGHT_BROWSERS_PATH=/home/vscode/.cache/playwright
ENV PATH=${NVM_DIR}/versions/node/v${NODE_VERSION}/bin:$PATH

# Set working directory
WORKDIR /workspace

# Copy gradle wrapper and build files first (for caching)
COPY --chown=vscode:vscode gradle gradle
COPY --chown=vscode:vscode gradlew gradlew.bat gradle.properties settings.gradle build.gradle ./
COPY --chown=vscode:vscode lombok.config ./

# Copy backend build configuration
COPY --chown=vscode:vscode backend/build.gradle backend/

# Download dependencies
USER vscode
RUN ./gradlew --no-daemon dependencies

# Copy package.json files for npm dependencies (for caching)
COPY --chown=vscode:vscode package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY --chown=vscode:vscode apps/frontend-v3/package.json apps/frontend-v3/
COPY --chown=vscode:vscode packages/ui/package.json packages/ui/
COPY --chown=vscode:vscode packages/e2e/package.json packages/e2e/

# Install pnpm and dependencies
RUN . "${NVM_DIR}/nvm.sh" \
    && npm install -g pnpm@latest \
    && pnpm install --frozen-lockfile

# Copy all source code
COPY --chown=vscode:vscode . .

# Build backend (skip tests for faster build)
RUN ./gradlew --no-daemon build -x test

# Install Playwright browsers
RUN . "${NVM_DIR}/nvm.sh" \
    && pnpm --filter e2e exec playwright install --with-deps

# Stage 3: Runtime image
FROM base AS runtime

# Set environment variables
ENV GRADLE_USER_HOME=/home/vscode/.gradle \
    NVM_DIR=/home/vscode/.nvm \
    TZ=Asia/Tokyo \
    SPRING_PROFILES_ACTIVE=dev \
    HOST=0.0.0.0 \
    PLAYWRIGHT_BROWSERS_PATH=/home/vscode/.cache/playwright
ENV PATH=${NVM_DIR}/versions/node/v${NODE_VERSION}/bin:$PATH

# Set timezone
USER root
RUN ln -sf /usr/share/zoneinfo/Asia/Tokyo /etc/localtime \
    && echo 'Asia/Tokyo' > /etc/timezone

# Set working directory
WORKDIR /workspace

# Copy built artifacts and dependencies from builder stage
COPY --from=builder --chown=vscode:vscode /home/vscode/.gradle /home/vscode/.gradle
COPY --from=builder --chown=vscode:vscode /home/vscode/.cache/playwright /home/vscode/.cache/playwright
COPY --from=builder --chown=vscode:vscode /home/vscode/.nvm /home/vscode/.nvm
COPY --from=builder --chown=vscode:vscode /workspace /workspace

# Expose ports
# 3000: Backend API
# 5173: Frontend v3 (Vite)
# 9323: Playwright Report
EXPOSE 3000 5173 9323

# Switch to vscode user
USER vscode

# Health check for backend
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:3000/mipla2/actuator/health || exit 1

# Default command (can be overridden)
CMD ["bash"]
