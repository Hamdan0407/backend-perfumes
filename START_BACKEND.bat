@echo off
title Backend Server
color 0A

echo ========================================
echo   STARTING BACKEND SERVER
echo ========================================
echo.

cd /d %~dp0

REM Clear old database
del /F /Q data\*.db 2>nul

REM Start backend
echo Starting Spring Boot...
echo.
java -jar target\perfume-shop-1.0.0.jar

pause
