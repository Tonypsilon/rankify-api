#!/bin/bash

# Containerized build script for rankify-api
# Uses a Maven container to build the project without requiring local Maven installation

set -e

CONTAINER_ENGINE=${CONTAINER_ENGINE:-podman}
BUILD_ARGS=${1:-"clean install"}

echo "Building rankify-api using $CONTAINER_ENGINE..."
echo "Build arguments: mvn $BUILD_ARGS"

# Check if we're in a sandboxed environment and handle accordingly
if [ -f /.dockerenv ] || [ -n "${GITHUB_ACTIONS}" ] || [ -n "${CI}" ]; then
    echo "Detected sandboxed/CI environment, using local Maven as fallback..."
    if command -v mvn &> /dev/null; then
        mvn $BUILD_ARGS
        echo "Build completed successfully using local Maven!"
        exit 0
    else
        echo "Warning: Local Maven not available and containerized build failed in sandbox environment"
        echo "In a normal development environment, this would use a Maven container"
    fi
fi

# Handle path conversion for Git Bash on Windows
PROJECT_PATH="$(pwd)"

# Debug: Show original path
echo "Original path from pwd: $PROJECT_PATH"

# Check if we're in Git Bash (MSYS/MinGW environment) or WSL
if [[ "$MSYSTEM" =~ ^MINGW ]] || [[ "$PROJECT_PATH" =~ ^/[a-zA-Z]/ ]] || [[ "$PROJECT_PATH" =~ ^/mnt/[a-zA-Z]/ ]]; then
    # In Git Bash, pwd returns paths like /c/Users/...
    # In WSL, pwd returns paths like /mnt/c/Users/...
    # Convert both to Windows format for Docker volume mounting
    if [[ "$PROJECT_PATH" =~ ^/mnt/ ]]; then
        # WSL format: /mnt/c/Users/... -> C:/Users/...
        PROJECT_PATH=$(echo "$PROJECT_PATH" | sed 's|^/mnt/\([a-zA-Z]\)/|\U\1:/|')
        echo "WSL detected, converted path: $PROJECT_PATH"
    else
        # Git Bash format: /c/Users/... -> C:/Users/...
        PROJECT_PATH=$(echo "$PROJECT_PATH" | sed 's|^/\([a-zA-Z]\)/|\U\1:/|')
        echo "Git Bash detected, converted path: $PROJECT_PATH"
    fi
fi

# Additional safety check - ensure we have a proper Windows path
if [[ ! "$PROJECT_PATH" =~ ^[A-Z]:/ ]]; then
    echo "ERROR: Failed to convert to proper Windows path format!"
    echo "Current PROJECT_PATH: $PROJECT_PATH"
    echo "Expected format: C:/path/to/project"
    exit 1
fi

echo "Final mounting path: $PROJECT_PATH -> /workspace (inside container)"

# Use Maven container to build the project
# HOST path gets mounted to /workspace INSIDE the container
echo "Running: $CONTAINER_ENGINE run --rm -v \"$PROJECT_PATH:/workspace\" -w /workspace ..."

# Use MSYS_NO_PATHCONV to prevent Git Bash from converting /workspace to Windows path
MSYS_NO_PATHCONV=1 $CONTAINER_ENGINE run --rm \
    -v "$PROJECT_PATH:/workspace" \
    -w /workspace \
    docker.io/library/maven:3.9.9-eclipse-temurin-24 \
    mvn $BUILD_ARGS

echo "Build completed successfully!"
