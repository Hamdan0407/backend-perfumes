@echo off
REM Automated production deployment for Perfume Shop (Windows)
REM Usage: deploy.bat

ECHO [1/4] Building and starting all services...
docker compose up --build -d

ECHO [2/4] Running database migrations (if any)...
REM Add migration command here if needed (e.g., Flyway, Liquibase)

REM ECHO [3/4] Seeding initial data (optional)...
REM docker compose exec backend java -jar app.jar --seed

ECHO [4/4] Checking service status...
docker compose ps

ECHO Deployment complete!
ECHO Frontend: http://localhost
ECHO Backend API: http://localhost/api
