# 🚀 Quick Start Commands
## Essential Commands for CAFM Backend Development

---

## 🎯 Initial Setup (One Time Only)

```bash
# 1. Clone the repository
git clone <repository-url>
cd cafm-backend

# 2. Set up environment variables
cp .env.example .env
# Edit .env file with your preferred editor

# 3. Make setup script executable
chmod +x scripts/docker-setup.sh

# 4. Run automated setup
./scripts/docker-setup.sh

# 5. Verify all services are running
docker ps
# Should show: cafm-postgres, cafm-redis, cafm-minio
```

---

## 📦 Daily Development Commands

### Starting Your Environment

```bash
# Start all services (if not running)
docker compose up -d

# Or use Docker Compose v1 if v2 not available
docker-compose up -d

# Start with logs visible
docker compose up

# Start specific services only
docker compose up -d postgres redis
```

### Stopping Your Environment

```bash
# Stop all services (preserves data)
docker compose down

# Stop and remove all data (fresh start)
docker compose down -v

# Stop specific service
docker compose stop postgres
```

---

## 🔍 Service Access Commands

### PostgreSQL Database

```bash
# Connect to PostgreSQL
docker exec -it cafm-postgres psql -U cafm_user -d cafm_db

# Quick database check
docker exec cafm-postgres pg_isready -U cafm_user

# Backup database
docker exec cafm-postgres pg_dump -U cafm_user cafm_db > backup_$(date +%Y%m%d).sql

# Restore database
docker exec -i cafm-postgres psql -U cafm_user cafm_db < backup.sql
```

### Redis Cache

```bash
# Connect to Redis CLI
docker exec -it cafm-redis redis-cli -a redis_password_2024

# Quick Redis check
docker exec cafm-redis redis-cli -a redis_password_2024 ping

# Clear all cache
docker exec cafm-redis redis-cli -a redis_password_2024 FLUSHALL

# Monitor Redis commands (real-time)
docker exec -it cafm-redis redis-cli -a redis_password_2024 MONITOR
```

### MinIO Storage

```bash
# Access MinIO Console in browser
open http://localhost:9001
# Login: minio_admin / minio_password_2024

# List buckets via CLI
docker run --rm --network cafm-backend_cafm-network \
  minio/mc:latest \
  --host http://minio_admin:minio_password_2024@minio:9000 \
  ls minio/

# Upload file to MinIO
docker run --rm -v $(pwd):/data --network cafm-backend_cafm-network \
  minio/mc:latest \
  --host http://minio_admin:minio_password_2024@minio:9000 \
  cp /data/myfile.pdf minio/cafm-files/
```

---

## 🛠️ Spring Boot Application Commands

### Running the Application

```bash
# Run Spring Boot (with Docker services running)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Run with debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Run tests
mvn test

# Run with coverage
mvn clean test jacoco:report
```

### Building the Application

```bash
# Clean and build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Package as JAR
mvn clean package

# Run JAR directly
java -jar target/cafm-backend-1.0.0.jar
```

---

## 📊 Monitoring & Logs

### View Logs

```bash
# All services logs
docker compose logs -f

# Specific service logs
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f minio

# Last 100 lines
docker compose logs --tail=100 postgres

# Logs since timestamp
docker compose logs --since 2024-01-01T10:00:00 postgres
```

### Check Service Health

```bash
# Check all container status
docker compose ps

# Check specific service health
docker inspect cafm-postgres | grep -A 5 "Health"

# Resource usage
docker stats

# Network inspection
docker network inspect cafm-backend_cafm-network
```

---

## 🧹 Maintenance Commands

### Clean Up

```bash
# Stop and remove containers
docker compose down

# Remove containers and volumes (full cleanup)
docker compose down -v

# Remove unused Docker resources
docker system prune -a

# Remove specific volume
docker volume rm cafm-backend_postgres_data
```

### Restart Services

```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart postgres

# Recreate containers
docker compose up -d --force-recreate

# Pull latest images and recreate
docker compose pull
docker compose up -d --force-recreate
```

---

## 🔧 Troubleshooting Commands

### Common Issues

```bash
# Service won't start - check logs
docker compose logs postgres

# Port already in use - find process
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9000  # MinIO

# Kill process using port
kill -9 $(lsof -t -i:5432)

# Connection refused - check container IP
docker inspect cafm-postgres | grep IPAddress

# Disk space issues
docker system df
docker system prune -a --volumes

# Permission issues
sudo chown -R $(whoami):$(whoami) .
```

### Reset Everything

```bash
# Complete reset (WARNING: Deletes all data!)
docker compose down -v
rm -rf postgres_data redis_data minio_data
docker system prune -a
./scripts/docker-setup.sh
```

---

## 🎯 Development Workflow

### Typical Daily Workflow

```bash
# 1. Start your day
cd cafm-backend
docker compose up -d
docker compose ps  # Verify all running

# 2. Check logs for any issues
docker compose logs --tail=50

# 3. Run your application
mvn spring-boot:run

# 4. Make changes and test
# ... develop ...
mvn test

# 5. End of day
mvn clean
docker compose down
```

### Quick Health Check

```bash
# Run this to verify everything is working
echo "Checking services..."
docker exec cafm-postgres pg_isready -U cafm_user && echo "✅ PostgreSQL: OK" || echo "❌ PostgreSQL: Failed"
docker exec cafm-redis redis-cli -a redis_password_2024 ping > /dev/null && echo "✅ Redis: OK" || echo "❌ Redis: Failed"
curl -f http://localhost:9000/minio/health/live > /dev/null 2>&1 && echo "✅ MinIO: OK" || echo "❌ MinIO: Failed"
```

---

## 📝 Aliases for Efficiency

Add these to your `.bashrc` or `.zshrc`:

```bash
# CAFM Backend aliases
alias cafm-up='docker compose up -d'
alias cafm-down='docker compose down'
alias cafm-logs='docker compose logs -f'
alias cafm-ps='docker compose ps'
alias cafm-db='docker exec -it cafm-postgres psql -U cafm_user -d cafm_db'
alias cafm-redis='docker exec -it cafm-redis redis-cli -a redis_password_2024'
alias cafm-run='mvn spring-boot:run'
alias cafm-test='mvn test'
alias cafm-clean='mvn clean && docker compose down'
```

---

## 🔑 Important URLs & Credentials

| Service | URL | Username | Password |
|---------|-----|----------|----------|
| Spring Boot App | http://localhost:8080 | - | - |
| MinIO Console | http://localhost:9001 | minio_admin | minio_password_2024 |
| PostgreSQL | localhost:5432 | cafm_user | cafm_password_2024 |
| Redis | localhost:6379 | - | redis_password_2024 |
| pgAdmin* | http://localhost:5050 | admin@cafm.com | pgadmin_password_2024 |
| Redis Commander* | http://localhost:8081 | - | - |
| Mailhog* | http://localhost:8025 | - | - |

*Only available with `--with-tools` flag

---

## 🆘 Emergency Commands

```bash
# If everything is broken
docker compose down -v
docker system prune -a --force
rm -rf postgres_data redis_data minio_data
./scripts/docker-setup.sh

# Check Docker daemon
docker version
systemctl status docker  # Linux
open -a Docker  # macOS - starts Docker Desktop

# Force remove stuck container
docker rm -f cafm-postgres

# Reset Docker to factory defaults (Nuclear option!)
# Docker Desktop -> Preferences -> Reset -> Reset to factory defaults
```

---

Last Updated: [Date]
Keep this handy for quick reference!