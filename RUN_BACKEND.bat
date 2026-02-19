@echo off
REM Start Backend with Email Support
REM This script starts the Perfume Shop backend in a way that prevents accidental interruption

echo.
echo ========================================
echo   PERFUME SHOP - BACKEND STARTUP
echo ========================================
echo.

REM Kill any existing Java processes
echo Stopping any existing processes...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 2 /nobreak

REM Set email credentials
set MAIL_USERNAME=hamdaan0615@gmail.com
set MAIL_PASSWORD=bqxcpmlqsgtotqqq

REM Change to project directory
cd /d "C:\Users\Hamdaan\OneDrive\Documents\maam"

REM Clear screen and show status
cls
echo.
echo ========================================
echo   STARTING BACKEND WITH EMAIL SUPPORT
echo ========================================
echo.
echo Email configured: hamdaan0615@gmail.com
echo Database: H2 (./data/perfume_shop.mv.db)
echo Server port: 8080
echo Frontend URL: http://localhost:3000
echo.
echo WAIT FOR: "Started PerfumeShopApplication in XX.XX seconds"
echo.
echo This window will MINIMIZE when ready!
echo ========================================
echo.

REM Start Maven Spring Boot
mvn spring-boot:run -DskipTests

REM Keep window open if it closes unexpectedly
pause
