#!/bin/bash

# Docker Setup Script for CAFM Backend
# This script sets up all required Docker containers for development

set -e

echo "🚀 Setting up CAFM Backend Docker Environment..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker Desktop first."
    echo "Visit: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Check if Docker Compose is installed (try both commands)
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose."
    exit 1
fi

# Set docker compose command (v2 vs v1)
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

echo "✅ Using Docker Compose: $DOCKER_COMPOSE"

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "⚠️  Please update .env file with your configuration"
fi

# Create necessary directories
echo "📁 Creating necessary directories..."
mkdir -p init-scripts
mkdir -p logs
mkdir -p data

# Create PostgreSQL initialization script
echo "📄 Creating PostgreSQL initialization script..."
cat > init-scripts/01-init.sql << 'EOF'
-- Initial database setup
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS cafm;

-- Set default schema
SET search_path TO cafm, public;

-- Create initial tables (will be managed by Flyway later)
CREATE TABLE IF NOT EXISTS cafm.database_info (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO cafm.database_info (version) VALUES ('1.0.0');

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA cafm TO cafm_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA cafm TO cafm_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA cafm TO cafm_user;

-- Create audit function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

COMMENT ON FUNCTION update_updated_at_column() IS 'Trigger function to update updated_at timestamp';
EOF

# Stop any existing containers
echo "🛑 Stopping existing containers..."
$DOCKER_COMPOSE down

# Pull latest images
echo "📦 Pulling latest Docker images..."
$DOCKER_COMPOSE pull

# Start core services
echo "🔧 Starting core services (PostgreSQL, Redis, MinIO)..."
$DOCKER_COMPOSE up -d postgres redis minio

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
until $DOCKER_COMPOSE exec -T postgres pg_isready -U cafm_user -d cafm_db > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✅"

# Wait for Redis to be ready
echo "⏳ Waiting for Redis to be ready..."
until $DOCKER_COMPOSE exec -T redis redis-cli -a redis_password_2024 ping > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✅"

# Wait for MinIO to be ready
echo "⏳ Waiting for MinIO to be ready..."
until curl -f http://localhost:9000/minio/health/live > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✅"

# Wait for MinIO and create buckets
echo "🪣 Setting up MinIO buckets..."
sleep 5  # Give MinIO more time to fully initialize

# Configure mc client and create buckets
docker run --rm --network cafm-backend_cafm-network \
    --entrypoint sh \
    minio/mc:latest -c '
    mc alias set minio http://minio:9000 minio_admin minio_password_2024 &&
    mc mb minio/cafm-files --ignore-existing &&
    mc mb minio/cafm-images --ignore-existing &&
    mc anonymous set public minio/cafm-files &&
    mc anonymous set public minio/cafm-images &&
    echo "✅ MinIO buckets created successfully"' || echo "⚠️ MinIO bucket creation failed (may already exist)"

# Start optional tools if requested
if [ "$1" == "--with-tools" ]; then
    echo "🛠️ Starting optional tools (pgAdmin, Redis Commander, Mailhog)..."
    $DOCKER_COMPOSE --profile tools up -d
fi

# Display service URLs
echo ""
echo "✅ Docker environment is ready!"
echo ""
echo "📌 Service URLs:"
echo "  • PostgreSQL:      localhost:5432"
echo "  • Redis:           localhost:6379"
echo "  • MinIO API:       http://localhost:9000"
echo "  • MinIO Console:   http://localhost:9001"

if [ "$1" == "--with-tools" ]; then
    echo "  • pgAdmin:         http://localhost:5050"
    echo "  • Redis Commander: http://localhost:8081"
    echo "  • Mailhog:         http://localhost:8025"
fi

echo ""
echo "📖 Quick Commands:"
echo "  • View logs:       $DOCKER_COMPOSE logs -f [service]"
echo "  • Stop services:   $DOCKER_COMPOSE down"
echo "  • Restart service: $DOCKER_COMPOSE restart [service]"
echo "  • Access PostgreSQL: docker exec -it cafm-postgres psql -U cafm_user -d cafm_db"
echo "  • Access Redis:    docker exec -it cafm-redis redis-cli -a redis_password_2024"
echo ""
echo "🎉 Happy coding!"