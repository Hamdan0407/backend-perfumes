@echo off
REM ============================================
REM   REBUILD DOCKER WITH LATEST CHANGES
REM ============================================

echo.
echo ============================================
echo    REBUILDING DOCKER CONTAINERS
echo ============================================
echo.

REM Stop and remove existing containers
echo [1/5] Stopping existing containers...
docker-compose down
if errorlevel 1 (
    echo Warning: docker-compose down failed. Continuing...
)

echo.
echo [2/5] Removing old images to force rebuild...
docker images | findstr "perfume" > nul
if not errorlevel 1 (
    for /f "tokens=3" %%i in ('docker images ^| findstr "perfume"') do (
        docker rmi -f %%i 2>nul
    )
)

echo.
echo [3/5] Building frontend with latest UI changes...
cd frontend
call npm run build
if errorlevel 1 (
    echo ERROR: Frontend build failed!
    pause
    exit /b 1
)
cd ..

echo.
echo [4/5] Building Docker images (this may take a few minutes)...
docker-compose build --no-cache
if errorlevel 1 (
    echo ERROR: Docker build failed!
    pause
    exit /b 1
)

echo.
echo [5/5] Starting containers...
docker-compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start containers!
    pause
    exit /b 1
)

echo.
echo ============================================
echo   REBUILD COMPLETE!
echo ============================================
echo.
echo Frontend: http://localhost:3000
echo Backend:  http://localhost:8080
echo.
echo Run 'docker-compose logs -f' to view logs
echo.

pause
