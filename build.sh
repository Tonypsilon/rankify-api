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

# Use Maven container to build the project
$CONTAINER_ENGINE run --rm \
    -v .:/workspace \
    -w /workspace \
    docker.io/library/maven:3.9.9-eclipse-temurin-24 \
    mvn $BUILD_ARGS

echo "Build completed successfully!"