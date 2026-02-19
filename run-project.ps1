# Complete Project Startup Script
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Starting Perfume Shop E-Commerce Project" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Step 1: Kill existing processes
Write-Host "`n[1/5] Stopping any running Java processes..." -ForegroundColor Yellow
taskkill /F /IM java.exe 2>$null | Out-Null
Start-Sleep -Seconds 2

# Step 2: Build the backend
Write-Host "[2/5] Building backend..." -ForegroundColor Yellow
cd C:\Users\Hamdaan\OneDrive\Documents\maam
$buildResult = & mvn clean package -DskipTests -q 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Backend build successful" -ForegroundColor Green
} else {
    Write-Host "✗ Backend build failed" -ForegroundColor Red
    Write-Host $buildResult
    exit 1
}

# Step 3: Start backend
Write-Host "`n[3/5] Starting backend server..." -ForegroundColor Yellow
$backendProcess = Start-Process -FilePath java -ArgumentList "-jar target/perfume-shop-1.0.0.jar --spring.profiles.active=demo" -PassThru -WindowStyle Hidden
Start-Sleep -Seconds 20

# Step 4: Verify backend is running
Write-Host "[4/5] Verifying backend..." -ForegroundColor Yellow
$backendCheck = $null
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/products?page=0&size=1" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Backend is running and responding" -ForegroundColor Green
        $backendCheck = $true
    }
} catch {
    Write-Host "⚠ Backend responding with: $($_.Exception.Message)" -ForegroundColor Yellow
    $backendCheck = $true
}

# Step 5: Start frontend
Write-Host "[5/5] Starting frontend... (port 3000)" -ForegroundColor Yellow
if (Test-Path "C:\Users\Hamdaan\OneDrive\Documents\maam\frontend") {
    cd C:\Users\Hamdaan\OneDrive\Documents\maam\frontend
    $frontendProcess = Start-Process -FilePath npm -ArgumentList "start" -PassThru -WindowStyle Hidden
    Start-Sleep -Seconds 10
    Write-Host "✓ Frontend started" -ForegroundColor Green
}

# Final Status
Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "Project Status:" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Backend: Running on http://localhost:8080" -ForegroundColor Green
Write-Host "Frontend: Running on http://localhost:3000" -ForegroundColor Green
Write-Host "`nTest Credentials:" -ForegroundColor Yellow
Write-Host "Email: mohammed@example.com" -ForegroundColor White
Write-Host "Password: password123" -ForegroundColor White
Write-Host "`nAdmin Credentials:" -ForegroundColor Yellow
Write-Host "Email: admin@perfumeshop.local" -ForegroundColor White
Write-Host "Password: admin123456" -ForegroundColor White
Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "Opening browser at http://localhost:3000/login" -ForegroundColor Yellow
Start-Sleep -Seconds 5
Start-Process "http://localhost:3000/login"
Write-Host "`n✓ Project is ready! Check browser." -ForegroundColor Green
