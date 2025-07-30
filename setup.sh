#!/bin/bash

# Setup script for rankify-api development environment
# Provides one-command setup for the complete development environment

set -e

CONTAINER_ENGINE=${CONTAINER_ENGINE:-podman}

# Determine compose command
if [ "$CONTAINER_ENGINE" = "podman" ]; then
    if command -v podman &> /dev/null && podman compose version &> /dev/null; then
        COMPOSE_COMMAND="podman compose"
    elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
        echo "Warning: podman compose not found, using docker compose as fallback"
        COMPOSE_COMMAND="docker compose"
        CONTAINER_ENGINE="docker"
    else
        echo "Error: Neither podman compose nor docker compose found"
        echo "Please install podman with compose support or docker compose"
        exit 1
    fi
else
    COMPOSE_COMMAND="docker compose"
fi

echo "Setting up rankify-api development environment..."
echo "Using container engine: $CONTAINER_ENGINE"
echo "Using compose command: $COMPOSE_COMMAND"

# Build the application
echo "Step 1/3: Building the application..."
export CONTAINER_ENGINE
./build.sh

# Build the application container
echo "Step 2/3: Building application container..."
$CONTAINER_ENGINE build -f Containerfile -t rankify-api:latest .

# Start the complete environment
echo "Step 3/3: Starting the development environment..."
$COMPOSE_COMMAND up -d

echo ""
echo "🎉 Development environment setup complete!"
echo ""
echo "Services status:"
$COMPOSE_COMMAND ps
echo ""
echo "📊 Access your application:"
echo "   • Application: http://localhost:8080"
echo "   • Health check: http://localhost:8080/actuator/health"
echo "   • Database: localhost:5432 (user: rankify, password: rankify, database: rankify)"
echo ""
echo "🔧 Useful commands:"
echo "   • View logs: $COMPOSE_COMMAND logs -f"
echo "   • Stop environment: $COMPOSE_COMMAND down"
echo "   • Rebuild after code changes: ./build.sh && $CONTAINER_ENGINE build -f Containerfile -t rankify-api:latest . && $COMPOSE_COMMAND up -d app"