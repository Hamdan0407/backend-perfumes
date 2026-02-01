@echo off
title Perfume Shop - Starting Application
color 0A

echo ============================================
echo    PERFUME SHOP - AUTO STARTER
echo ============================================
echo.

cd /d "%~dp0"

:: Kill any existing processes on ports 8080 and 3000
echo [1/5] Cleaning up old processes...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do taskkill /PID %%a /F >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING 2^>nul') do taskkill /PID %%a /F >nul 2>&1
timeout /t 2 /nobreak >nul

:: Remove any H2 lock files
echo [2/5] Removing database locks...
del /q "data\*.lock.db" 2>nul

:: Check if JAR exists, if not build it
echo [3/5] Checking backend JAR...
if not exist "target\perfume-shop-1.0.0.jar" (
    echo Building backend... This may take a minute...
    call C:\apache-maven\bin\mvn.cmd clean package -DskipTests -q
    echo Build complete!
)

:: Start Backend in new window
echo [4/5] Starting Backend on port 8080...
start "Perfume Shop Backend" cmd /k "cd /d %~dp0 && java -Dspring.profiles.active=dev -jar target\perfume-shop-1.0.0.jar"

:: Wait for backend to start
echo Waiting for backend to initialize (15 seconds)...
timeout /t 15 /nobreak >nul

:: Start Frontend in new window
echo [5/5] Starting Frontend on port 3000...
start "Perfume Shop Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"

:: Wait for frontend
timeout /t 5 /nobreak >nul

echo.
echo ============================================
echo    APPLICATION STARTED SUCCESSFULLY!
echo ============================================
echo.
echo    Frontend: http://localhost:3000
echo    Backend:  http://localhost:8080
echo.
echo    Opening browser in 3 seconds...
echo ============================================

timeout /t 3 /nobreak >nul
start http://localhost:3000

echo.
echo Press any key to close this launcher (servers keep running)...
pause >nul
