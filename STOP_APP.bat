@echo off
title Perfume Shop - Stopping Application
color 0C

echo ============================================
echo    PERFUME SHOP - STOPPING SERVERS
echo ============================================
echo.

echo Stopping all Java processes (Backend)...
taskkill /IM java.exe /F >nul 2>&1

echo Stopping Node processes (Frontend)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING 2^>nul') do taskkill /PID %%a /F >nul 2>&1

echo Removing database lock files...
del /q "data\*.lock.db" 2>nul

echo.
echo ============================================
echo    ALL SERVERS STOPPED
echo ============================================
echo.
pause
