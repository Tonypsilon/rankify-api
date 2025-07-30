#!/bin/bash

# Test script to verify development environment components
# Tests each component individually to ensure they work correctly

set -e

CONTAINER_ENGINE=${CONTAINER_ENGINE:-podman}

echo "🧪 Testing rankify-api development environment components..."
echo ""

# Test 1: Containerized build
echo "Test 1: Containerized Maven build"
echo "================================="
if ./build.sh "clean install"; then
    echo "✅ Containerized build: PASSED"
else
    echo "❌ Containerized build: FAILED"
    exit 1
fi
echo ""

# Test 2: Application container build
echo "Test 2: Application container build"
echo "=================================="
if $CONTAINER_ENGINE build -f Containerfile -t rankify-api:test .; then
    echo "✅ Application container build: PASSED"
else
    echo "❌ Application container build: FAILED"
    exit 1
fi
echo ""

# Test 3: Database container
echo "Test 3: Database container startup"
echo "================================="
if $CONTAINER_ENGINE run -d --name test-db \
    -e POSTGRES_DB=rankify \
    -e POSTGRES_USER=rankify \
    -e POSTGRES_PASSWORD=rankify \
    -p 5433:5432 \
    docker.io/library/postgres:16-alpine; then
    
    echo "Waiting for database to be ready..."
    sleep 10
    
    if $CONTAINER_ENGINE exec test-db pg_isready -U rankify -d rankify; then
        echo "✅ Database container: PASSED"
    else
        echo "❌ Database container: FAILED"
        $CONTAINER_ENGINE stop test-db && $CONTAINER_ENGINE rm test-db
        exit 1
    fi
    
    # Cleanup
    $CONTAINER_ENGINE stop test-db && $CONTAINER_ENGINE rm test-db
else
    echo "❌ Database container: FAILED"
    exit 1
fi
echo ""

# Test 4: Compose file validation
echo "Test 4: Compose file validation"
echo "==============================="
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
elif command -v podman &> /dev/null && podman compose version &> /dev/null; then
    COMPOSE_CMD="podman compose"
else
    echo "⚠️ Compose file validation: SKIPPED (no compose tool found)"
    echo ""
    echo "🎉 All available tests passed!"
    echo ""
    echo "Summary:"
    echo "- Containerized build works correctly"
    echo "- Application container builds successfully"
    echo "- Database container starts and responds to health checks"
    echo "- Environment is ready for development"
    exit 0
fi

if $COMPOSE_CMD config > /dev/null 2>&1; then
    echo "✅ Compose file validation: PASSED"
else
    echo "❌ Compose file validation: FAILED"
    exit 1
fi
echo ""

echo "🎉 All tests passed!"
echo ""
echo "Summary:"
echo "- Containerized build works correctly"
echo "- Application container builds successfully"
echo "- Database container starts and responds to health checks"
echo "- Compose configuration is valid"
echo "- Environment is ready for development"