@echo off
echo.
echo ========================================
echo   Starting Perfume Shop
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Building backend...
call apache-maven-3.9.6\bin\mvn.cmd package -DskipTests -q

if not exist "target\perfume-shop-1.0.0.jar" (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo [2/3] Starting backend...
start "Backend" cmd /k "java -jar target\perfume-shop-1.0.0.jar"

echo [3/3] Backend started in separate window
echo.
echo ========================================
echo   WAIT 50 SECONDS THEN:
echo   Open: http://localhost:3000
echo ========================================
echo.
pause
