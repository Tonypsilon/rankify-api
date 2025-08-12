#!/bin/bash

# reset-db-only.sh - Quick database-only reset for rapid development
# Keeps application container running, only resets database

set -e

echo "🗑️  Resetting database only..."

# Stop and remove database container and volume only
echo "Removing database container and volume..."
podman-compose stop database
podman-compose rm -f database
podman volume rm -f rankify-api_postgres_data 2>/dev/null || true

# Restart just the database
echo "🚀 Starting fresh database..."
podman-compose up -d database

# Wait for database to be ready
echo "⏳ Waiting for database..."
for i in {1..20}; do
    if podman-compose exec database pg_isready -U rankify -d rankify >/dev/null 2>&1; then
        echo "✅ Database is ready!"
        break
    fi
    if [ $i -eq 20 ]; then
        echo "❌ Database failed to start"
        exit 1
    fi
    sleep 1
done

# Restart application to trigger Liquibase migration
echo "🔄 Restarting application to apply migrations..."
podman-compose restart app

# Wait for application
echo "⏳ Waiting for application..."
for i in {1..30}; do
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "✅ Application ready with fresh database!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Application failed to restart"
        exit 1
    fi
    sleep 2
done

echo "🎉 Database-only reset complete!"
podman-compose exec database psql -U rankify -d rankify -c "\dt"
