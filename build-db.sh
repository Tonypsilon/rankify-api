#!/bin/bash

# Build database management script for rankify-api
# Manages a separate PostgreSQL container specifically for building and testing

set -e

CONTAINER_ENGINE=${CONTAINER_ENGINE:-podman}
DB_CONTAINER_NAME="rankify-build-db"
DB_PORT="5432"
COMPOSE_DB_CONTAINER_NAME="rankify-database"

# Function to check if container exists
container_exists() {
    $CONTAINER_ENGINE ps -a --format "{{.Names}}" | grep -q "^${DB_CONTAINER_NAME}$"
}

# Function to check if container is running
container_running() {
    $CONTAINER_ENGINE ps --format "{{.Names}}" | grep -q "^${DB_CONTAINER_NAME}$"
}

# Function to check if compose database is running
compose_db_running() {
    $CONTAINER_ENGINE ps --format "{{.Names}}" | grep -q "^${COMPOSE_DB_CONTAINER_NAME}$"
}

# Function to check if port 5432 is in use
port_in_use() {
    if command -v ss >/dev/null 2>&1; then
        ss -ln | grep -q ":5432 "
    elif command -v netstat >/dev/null 2>&1; then
        netstat -ln | grep -q ":5432 "
    else
        # Fallback: try to bind to the port
        timeout 1 bash -c "</dev/tcp/localhost/5432" >/dev/null 2>&1
    fi
}

# Function to wait for database to be ready
wait_for_db() {
    echo "Waiting for database to be ready..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if $CONTAINER_ENGINE exec $DB_CONTAINER_NAME pg_isready -U rankify -d rankify_test >/dev/null 2>&1; then
            echo "Database is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: Database not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "ERROR: Database failed to become ready after $max_attempts attempts"
    return 1
}

case "${1:-start}" in
    start)
        if container_running; then
            echo "Build database container is already running"
            exit 0
        fi
        
        # Check for conflicts with compose database or other services using port 5432
        if compose_db_running; then
            echo "ERROR: Compose database container (rankify-database) is running on port 5432"
            echo "Please stop the compose environment first: docker compose down"
            exit 1
        fi
        
        if port_in_use; then
            echo "ERROR: Port 5432 is already in use by another service"
            echo "Please stop any other PostgreSQL services or use a different port"
            exit 1
        fi
        
        if container_exists; then
            echo "Starting existing build database container..."
            $CONTAINER_ENGINE start $DB_CONTAINER_NAME
        else
            echo "Creating and starting build database container..."
            $CONTAINER_ENGINE run -d --name $DB_CONTAINER_NAME \
                -e POSTGRES_DB=rankify_test \
                -e POSTGRES_USER=rankify \
                -e POSTGRES_PASSWORD=password \
                -p $DB_PORT:5432 \
                docker.io/library/postgres:16-alpine
        fi
        
        wait_for_db
        ;;
        
    stop)
        if container_running; then
            echo "Stopping build database container..."
            $CONTAINER_ENGINE stop $DB_CONTAINER_NAME
        else
            echo "Build database container is not running"
        fi
        ;;
        
    remove|rm)
        if container_running; then
            echo "Stopping build database container..."
            $CONTAINER_ENGINE stop $DB_CONTAINER_NAME
        fi
        
        if container_exists; then
            echo "Removing build database container..."
            $CONTAINER_ENGINE rm $DB_CONTAINER_NAME
        else
            echo "Build database container does not exist"
        fi
        ;;
        
    status)
        if container_running; then
            echo "Build database container is running"
            $CONTAINER_ENGINE ps --filter name=$DB_CONTAINER_NAME --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        elif container_exists; then
            echo "Build database container exists but is not running"
            $CONTAINER_ENGINE ps -a --filter name=$DB_CONTAINER_NAME --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        else
            echo "Build database container does not exist"
        fi
        ;;
        
    *)
        echo "Usage: $0 {start|stop|remove|status}"
        echo ""
        echo "Commands:"
        echo "  start   - Start the build database container (create if needed)"
        echo "  stop    - Stop the build database container"
        echo "  remove  - Stop and remove the build database container"
        echo "  status  - Show the status of the build database container"
        echo ""
        echo "Environment variables:"
        echo "  CONTAINER_ENGINE - Use 'docker' or 'podman' (default: podman)"
        exit 1
        ;;
esac