@echo off
cd /d "%~dp0"
echo.
echo ========================================
echo  Rebuilding with Wishlist Feature
echo ========================================
echo.

echo Stopping existing Java processes...
taskkill /F /IM java.exe 2>nul

timeout /t 3 >nul

echo Building backend...
call apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

if exist "target\perfume-shop-1.0.0.jar" (
    echo.
    echo ========================================
    echo  Build Successful!
    echo ========================================
    echo.
    echo Starting backend...
    echo.
    start "Backend-Wishlist" cmd /k "java -jar target\perfume-shop-1.0.0.jar"
    echo.
    echo Backend started in separate window
    echo Wait 50 seconds then try the wishlist feature!
    echo.
) else (
    echo.
    echo Build FAILED! Check errors above.
    echo.
    pause
)
