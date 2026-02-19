@echo off
REM Production Startup Script for Perfume Shop (Windows)
REM Author: System Administrator  
REM Last Updated: 2026-02-02

setlocal enabledelayedexpansion

echo.
echo 🚀 Starting Perfume Shop in PRODUCTION mode...
echo ================================================

REM Check if .env.production file exists
if not exist ".env.production" (
    echo ❌ ERROR: .env.production file not found!
    echo Please create .env.production with all required environment variables.
    echo Copy .env.production.example and update the values.
    pause
    exit /b 1
)

REM Load production environment variables from .env.production
echo 📋 Loading production environment variables...
for /f "usebackq tokens=1,* delims==" %%a in (".env.production") do (
    if not "%%a"=="" if not "%%a"=="#" if not "%%a"=="REM" (
        set "%%a=%%b"
        REM Don't echo sensitive values
        echo   - %%a=***
    )
)

REM Security validations
echo.
echo 🔒 Performing security checks...

if "%JWT_SECRET%"=="" (
    echo ❌ ERROR: JWT_SECRET is not set!
    pause
    exit /b 1
)

if "%JWT_SECRET:CHANGE_ME_=%"=="%JWT_SECRET%" goto jwt_ok
echo ❌ ERROR: JWT_SECRET is using default placeholder!
echo Please set a secure JWT_SECRET in .env.production
pause
exit /b 1
:jwt_ok

if "%DATABASE_PASSWORD%"=="" (
    echo ❌ ERROR: DATABASE_PASSWORD is not set!
    pause
    exit /b 1
)

if "%DATABASE_PASSWORD:CHANGE_ME_=%"=="%DATABASE_PASSWORD%" goto db_ok
echo ❌ ERROR: DATABASE_PASSWORD is using default placeholder!
echo Please set a strong DATABASE_PASSWORD in .env.production
pause
exit /b 1
:db_ok

if "%RAZORPAY_KEY_ID:_test_=%"=="%RAZORPAY_KEY_ID%" goto razorpay_ok
echo ❌ ERROR: Still using Razorpay TEST keys in production!
echo Please update to LIVE Razorpay keys in .env.production
pause
exit /b 1
:razorpay_ok

echo ✅ Security checks passed

REM Check if JAR file exists
set JAR_FILE=target\perfume-shop-1.0.0.jar
if not exist "%JAR_FILE%" (
    echo ❌ ERROR: JAR file not found: %JAR_FILE%
    echo Please run: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo ✅ JAR file found: %JAR_FILE%

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

echo.
echo 📋 Production Configuration:
echo   - Profile: production
echo   - Port: %PORT%
echo   - Database: %DATABASE_URL:*@=***@%
echo   - Redis: %REDIS_HOST%:%REDIS_PORT%
echo   - Log Level: %LOG_LEVEL%
echo   - JWT Expiry: %JWT_EXPIRATION%ms

echo.
echo 🚀 Starting application...
echo ================================================
echo Press Ctrl+C to stop the application
echo.

REM Start the application with production profile
java %JAVA_OPTS% ^
  -Dspring.profiles.active=production ^
  -Dfile.encoding=UTF-8 ^
  -jar "%JAR_FILE%" ^
  --server.port=%PORT%

echo.
echo 🛑 Application has stopped.
pause