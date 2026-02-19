# ============================================
#   REBUILD DOCKER WITH LATEST CHANGES
# ============================================

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "   REBUILDING DOCKER CONTAINERS" -ForegroundColor Cyan
Write-Host "============================================`n" -ForegroundColor Cyan

# Navigate to project root
Set-Location $PSScriptRoot

# Stop and remove existing containers
Write-Host "[1/5] Stopping existing containers..." -ForegroundColor Yellow
docker-compose down 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Warning: docker-compose down failed. Continuing..." -ForegroundColor Yellow
}

# Remove old images to force rebuild
Write-Host "`n[2/5] Removing old images to force rebuild..." -ForegroundColor Yellow
$images = docker images --format "{{.Repository}}:{{.Tag}}" | Select-String "perfume"
if ($images) {
    $images | ForEach-Object { 
        Write-Host "Removing $_" -ForegroundColor Gray
        docker rmi -f $_ 2>&1 | Out-Null 
    }
}

# Build frontend with latest changes
Write-Host "`n[3/5] Building frontend with latest UI changes..." -ForegroundColor Yellow
Set-Location frontend
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Frontend build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
Set-Location ..

# Build Docker images
Write-Host "`n[4/5] Building Docker images (this may take a few minutes)..." -ForegroundColor Yellow
docker-compose build --no-cache
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Docker build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Start containers
Write-Host "`n[5/5] Starting containers..." -ForegroundColor Yellow
docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nERROR: Failed to start containers!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "   REBUILD COMPLETE!" -ForegroundColor Green
Write-Host "============================================`n" -ForegroundColor Green

Write-Host "Frontend: " -NoNewline
Write-Host "http://localhost:3000" -ForegroundColor Cyan
Write-Host "Backend:  " -NoNewline
Write-Host "http://localhost:8080" -ForegroundColor Cyan
Write-Host "`nRun 'docker-compose logs -f' to view logs`n" -ForegroundColor Gray

# Wait for services to be healthy
Write-Host "Waiting for services to be healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Check service status
Write-Host "`nService Status:" -ForegroundColor Cyan
docker-compose ps

Write-Host "`nDone! Press Enter to exit..." -ForegroundColor Green
Read-Host
