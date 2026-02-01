#!/bin/bash
# Automated production deployment for Perfume Shop
# Usage: ./deploy.sh
set -e

echo "[1/4] Building and starting all services..."
docker compose up --build -d

echo "[2/4] Running database migrations (if any)..."
# Add migration command here if needed (e.g., Flyway, Liquibase)

# echo "[3/4] Seeding initial data (optional)..."
# docker compose exec backend java -jar app.jar --seed

echo "[4/4] Checking service status..."
docker compose ps

echo "Deployment complete!"
echo "Frontend: http://localhost"
echo "Backend API: http://localhost/api"
