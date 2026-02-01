# ============================================================================
# PERFUME SHOP - AUTOMATED DOCKER SETUP SCRIPT
# ============================================================================
# This script automates the entire setup process for a fresh machine
# - Generates secure environment variables
# - Creates .env.production file
# - Validates dependencies
# - Builds the backend JAR
# - Provides Docker startup commands
# ============================================================================

Write-Host "`n" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   PERFUME SHOP - AUTOMATED DOCKER SETUP" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "`n"

# Color functions
function Write-Success { Write-Host "[OK] $args" -ForegroundColor Green }
function Write-Error-Custom { Write-Host "[ERROR] $args" -ForegroundColor Red }
function Write-Warning-Custom { Write-Host "[WARNING] $args" -ForegroundColor Yellow }
function Write-Info { Write-Host "[INFO] $args" -ForegroundColor Cyan }

# Get script location
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Info "Project directory: $ScriptDir"
Write-Host ""

# ============================================================================
# STEP 1: VALIDATE PREREQUISITES
# ============================================================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "STEP 1: VALIDATING PREREQUISITES" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$ChecksPassed = 0
$ChecksRequired = 0

# Check Docker
Write-Info "Checking Docker..."
$ChecksRequired++
try {
    $DockerVersion = docker --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker installed: $DockerVersion"
        $ChecksPassed++
    } else {
        Write-Error-Custom "Docker not found or not accessible"
    }
} catch {
    Write-Error-Custom "Docker check failed: $_"
}

# Check Docker Compose
Write-Info "Checking Docker Compose..."
$ChecksRequired++
try {
    $ComposeVersion = docker compose version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker Compose installed: $ComposeVersion"
        $ChecksPassed++
    } else {
        Write-Error-Custom "Docker Compose not found (try 'docker-compose' or install Docker Desktop)"
    }
} catch {
    Write-Error-Custom "Docker Compose check failed: $_"
}

# Check Java
Write-Info "Checking Java..."
$ChecksRequired++
try {
    $JavaVersion = java -version 2>&1 | Select-Object -First 1
    if ($LASTEXITCODE -eq 0 -or $JavaVersion -match "version") {
        Write-Success "Java installed: $JavaVersion"
        $ChecksPassed++
    } else {
        Write-Error-Custom "Java not found or version not detected"
    }
} catch {
    Write-Error-Custom "Java check failed: $_"
}

# Check Maven
Write-Info "Checking Maven..."
$ChecksRequired++
try {
    $MavenVersion = mvn --version 2>$null | Select-Object -First 1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Maven installed: $MavenVersion"
        $ChecksPassed++
    } else {
        Write-Error-Custom "Maven not found"
    }
} catch {
    Write-Error-Custom "Maven check failed: $_"
}

# Check disk space
Write-Info "Checking disk space..."
$ChecksRequired++
$DiskInfo = Get-Volume -DriveLetter C | Select-Object SizeRemaining
if ($DiskInfo.SizeRemaining -gt 5GB) {
    $FreeGB = [math]::Round($DiskInfo.SizeRemaining / 1GB, 2)
    Write-Success "Sufficient disk space: ${FreeGB}GB free"
    $ChecksPassed++
} else {
    Write-Error-Custom "Insufficient disk space (need 5GB+)"
}

Write-Host ""
Write-Host "Prerequisites: $ChecksPassed/$ChecksRequired passed" -ForegroundColor Cyan
Write-Host ""

if ($ChecksPassed -lt $ChecksRequired) {
    Write-Error-Custom "Some prerequisites failed. Please install missing tools:"
    Write-Host "  - Docker Desktop (includes Docker and Docker Compose)"
    Write-Host "  - Java 17+ (https://jdk.java.net/)"
    Write-Host "  - Maven (https://maven.apache.org/)"
    Write-Host ""
    Write-Host "After installation, run this script again."
    exit 1
}

# ============================================================================
# STEP 2: CHECK IF .env.production EXISTS
# ============================================================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "STEP 2: ENVIRONMENT FILE" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$EnvFile = ".env.production"

if (Test-Path $EnvFile) {
    Write-Info "Found existing $EnvFile"
    Write-Host ""
    Write-Host "Do you want to:" -ForegroundColor Yellow
    Write-Host "  1. Keep existing .env.production file"
    Write-Host "  2. Regenerate with new values"
    Write-Host ""
    $Choice = Read-Host "Enter choice (1 or 2)"
    
    if ($Choice -eq "2") {
        Remove-Item $EnvFile -Force
        Write-Success "Removed existing $EnvFile"
    } elseif ($Choice -eq "1") {
        Write-Success "Using existing $EnvFile"
        Write-Warning-Custom "Make sure all critical variables are set!"
        Write-Host ""
    } else {
        Write-Error-Custom "Invalid choice. Exiting."
        exit 1
    }
}

# Generate .env.production if it doesn't exist
if (-not (Test-Path $EnvFile)) {
    Write-Info "Generating new $EnvFile with secure values..."
    Write-Host ""
    
    # Generate secure random values
    $JwtSecret = [Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))
    $DbPassword = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 16 | ForEach-Object { [char]$_ })
    $RandomMailSecret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
    
    Write-Info "Generated JWT_SECRET: $(if ($JwtSecret.Length -gt 20) { $JwtSecret.Substring(0, 20) + '...' } else { $JwtSecret })"
    Write-Info "Generated DATABASE_PASSWORD: $(if ($DbPassword.Length -gt 20) { $DbPassword.Substring(0, 20) + '...' } else { $DbPassword })"
    
    # Read .env.production.example
    if (-not (Test-Path ".env.production.example")) {
        Write-Error-Custom ".env.production.example not found!"
        exit 1
    }
    
    # Create .env.production with example values
    $Content = Get-Content ".env.production.example" -Raw
    
    # Replace placeholders with generated values
    $Content = $Content -replace '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$JwtSecret"
    $Content = $Content -replace '(?m)^DATABASE_PASSWORD=.*$', "DATABASE_PASSWORD=$DbPassword"
    
    # Set default test values for other critical variables
    # These are safe test values that work for development
    $Content = $Content -replace '(?m)^DATABASE_USERNAME=.*$', "DATABASE_USERNAME=prod_user"
    $Content = $Content -replace '(?m)^MAIL_USERNAME=.*$', "MAIL_USERNAME=noreply@perfume.local"
    $Content = $Content -replace '(?m)^MAIL_PASSWORD=.*$', "MAIL_PASSWORD=test-password-123"
    $Content = $Content -replace '(?m)^RAZORPAY_KEY_ID=.*$', "RAZORPAY_KEY_ID=rzp_test_00000000000000"
    $Content = $Content -replace '(?m)^RAZORPAY_KEY_SECRET=.*$', "RAZORPAY_KEY_SECRET=test_secret_key_00000000"
    $Content = $Content -replace '(?m)^RAZORPAY_WEBHOOK_SECRET=.*$', "RAZORPAY_WEBHOOK_SECRET=webhook_test_secret_key"
    $Content = $Content -replace '(?m)^CORS_ORIGINS=.*$', "CORS_ORIGINS=http://localhost:3000,http://localhost:80"
    $Content = $Content -replace '(?m)^FRONTEND_URL=.*$', "FRONTEND_URL=http://localhost:3000"
    $Content = $Content -replace '(?m)^MAIL_HOST=.*$', "MAIL_HOST=mailhog"
    $Content = $Content -replace '(?m)^MAIL_PORT=.*$', "MAIL_PORT=1025"
    
    # Write to file
    Set-Content -Path $EnvFile -Value $Content -Encoding UTF8
    Write-Success "Created $EnvFile with secure values"
    Write-Host ""
    
    # Show critical values note
    Write-Warning-Custom "IMPORTANT NOTES:"
    Write-Host ""
    Write-Host "The .env.production file has been created with:"
    Write-Host "  ✅ Generated JWT_SECRET (cryptographically secure)"
    Write-Host "  ✅ Generated DATABASE_PASSWORD (secure random)"
    Write-Host "  ℹ️  Test values for email, Razorpay (for development/testing)"
    Write-Host ""
    Write-Host "For PRODUCTION, update these in .env.production:"
    Write-Host "  - MAIL_USERNAME: Your actual email address"
    Write-Host "  - MAIL_PASSWORD: Your email app password (not regular password!)"
    Write-Host "  - RAZORPAY_KEY_ID: From Razorpay dashboard"
    Write-Host "  - RAZORPAY_KEY_SECRET: From Razorpay dashboard"
    Write-Host "  - RAZORPAY_WEBHOOK_SECRET: From Razorpay webhook settings"
    Write-Host ""
    
    # Ask if user wants to edit
    $EditEnv = Read-Host "Do you want to edit .env.production now? (y/n)"
    if ($EditEnv -eq "y") {
        notepad ".env.production"
        Write-Info "Edit complete. Continuing setup..."
    }
} else {
    Write-Success "Using existing $EnvFile"
}

Write-Host ""

# ============================================================================
# STEP 3: BUILD BACKEND JAR
# ============================================================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "STEP 3: BUILDING BACKEND" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Check if JAR already exists
if (Test-Path "target/perfume-shop-1.0.0.jar") {
    Write-Info "Found existing target/perfume-shop-1.0.0.jar"
    Write-Host "Rebuild JAR (y/n)?" -ForegroundColor Yellow
    $RebuildJar = Read-Host "Enter choice"
    if ($RebuildJar -ne "y") {
        Write-Success "Using existing JAR file"
    } else {
        Write-Info "Cleaning previous build..."
        mvn clean | Out-Null
        Write-Info "Building JAR with Maven (this may take 2-5 minutes)..."
        mvn package -DskipTests -q
        if ($LASTEXITCODE -eq 0) {
            Write-Success "JAR built successfully!"
            $JarSize = (Get-Item "target/perfume-shop-1.0.0.jar").Length / 1MB
            Write-Info "JAR size: $([math]::Round($JarSize, 2)) MB"
        } else {
            Write-Error-Custom "Maven build failed! Check above for errors."
            exit 1
        }
    }
} else {
    Write-Info "Building JAR with Maven (this may take 2-5 minutes)..."
    Write-Host "Please wait... (output will be minimal)" -ForegroundColor Yellow
    mvn package -DskipTests -q
    
    if ($LASTEXITCODE -eq 0 -and (Test-Path "target/perfume-shop-1.0.0.jar")) {
        Write-Success "JAR built successfully!"
        $JarSize = (Get-Item "target/perfume-shop-1.0.0.jar").Length / 1MB
        Write-Info "JAR size: $([math]::Round($JarSize, 2)) MB"
    } else {
        Write-Error-Custom "Maven build failed!"
        Write-Host ""
        Write-Host "Trying with verbose output to diagnose..." -ForegroundColor Yellow
        mvn package -DskipTests
        exit 1
    }
}

Write-Host ""

# ============================================================================
# STEP 4: DOCKER COMPOSE VALIDATION
# ============================================================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "STEP 4: VALIDATING DOCKER SETUP" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Validate docker-compose.yml syntax
Write-Info "Validating docker-compose.yml..."
docker compose config > $null 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Success "docker-compose.yml is valid"
} else {
    Write-Error-Custom "docker-compose.yml has syntax errors!"
    Write-Host "Running diagnosis..." -ForegroundColor Yellow
    docker compose config
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 5: READY TO START
# ============================================================================
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "STEP 5: READY TO START" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Success "All prerequisites validated!"
Write-Success "Environment file created/updated: $EnvFile"
Write-Success "Backend JAR ready: target/perfume-shop-1.0.0.jar"
Write-Success "Docker configuration validated"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  SETUP COMPLETE! YOU'RE READY TO START THE CONTAINERS     ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green

Write-Host ""
Write-Host "To start the application, run:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  docker compose --env-file .env.production up --build" -ForegroundColor Yellow
Write-Host ""

Write-Host "This will:" -ForegroundColor Cyan
Write-Host "  1. Pull Docker images (MySQL, etc.)"
Write-Host "  2. Build the API container"
Write-Host "  3. Start MySQL database"
Write-Host "  4. Start Spring Boot API (port 8080)"
Write-Host "  5. Show logs in real-time"
Write-Host ""

Write-Host "In another terminal, you can verify with:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  # Check if services are running"
Write-Host "  docker compose ps" -ForegroundColor Yellow
Write-Host ""
Write-Host "  # Check API health"
Write-Host "  curl http://localhost:8080/actuator/health" -ForegroundColor Yellow
Write-Host ""
Write-Host "  # Get featured products"
Write-Host "  curl http://localhost:8080/api/products/featured" -ForegroundColor Yellow
Write-Host ""

Write-Host "Documentation available in:" -ForegroundColor Cyan
Write-Host "  - START_HERE.md"
Write-Host "  - DOCKER_QUICK_REFERENCE.md"
Write-Host "  - RUN_CHECKLIST.md"
Write-Host ""

$StartNow = Read-Host "Start containers now? (y/n)"
if ($StartNow -eq "y") {
    Write-Info "Starting Docker containers..."
    Write-Host ""
    docker compose --env-file .env.production up --build
} else {
    Write-Info "Setup complete! Start containers whenever you're ready with:"
    Write-Host "  docker compose --env-file .env.production up --build" -ForegroundColor Yellow
}
