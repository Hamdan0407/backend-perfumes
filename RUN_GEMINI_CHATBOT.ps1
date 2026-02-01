# Perfume Shop - Gemini Chatbot Setup & Run Script
# This script builds and runs the entire application with Google Gemini API

Write-Host "🚀 Perfume Shop - Gemini Chatbot Deployment Script" -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan
Write-Host ""

# Set environment variables for Gemini
$env:GOOGLE_AI_API_KEY = "AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE"
$env:GOOGLE_PROJECT_ID = "perfume-shop"

Write-Host "✅ Environment Variables Set:" -ForegroundColor Green
Write-Host "   GOOGLE_AI_API_KEY: $($env:GOOGLE_AI_API_KEY.Substring(0,10))..." -ForegroundColor Yellow
Write-Host "   GOOGLE_PROJECT_ID: $env:GOOGLE_PROJECT_ID" -ForegroundColor Yellow
Write-Host ""

# Check Java
Write-Host "🔍 Checking Java Installation..." -ForegroundColor Cyan
java -version 2>&1 | Select-String "Java"
Write-Host ""

# Try to find and install Maven if needed
Write-Host "🔍 Checking Maven Installation..." -ForegroundColor Cyan
$mavenExists = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -eq $mavenExists) {
    Write-Host "⚠️  Maven not found. Installing via Chocolatey..." -ForegroundColor Yellow
    choco install maven -y
}

# Navigate to project directory
Set-Location "c:\Users\Hamdaan\Documents\maam"
Write-Host "📂 Working Directory: $(Get-Location)" -ForegroundColor Cyan
Write-Host ""

# Build backend
Write-Host "🔨 Building Backend with Maven..." -ForegroundColor Cyan
Write-Host "Command: mvn clean package -DskipTests" -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Maven build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Backend build successful!" -ForegroundColor Green
Write-Host ""

# Check if JAR was created
$jarFile = Get-ChildItem "target/*.jar" -Exclude "*sources.jar" 2>/dev/null | Select-Object -First 1
if ($null -eq $jarFile) {
    Write-Host "❌ JAR file not found after build!" -ForegroundColor Red
    exit 1
}

Write-Host "📦 JAR File: $($jarFile.Name)" -ForegroundColor Green
Write-Host "📊 File Size: $([math]::Round($jarFile.Length / 1MB, 2)) MB" -ForegroundColor Green
Write-Host ""

# Prompt user for next action
Write-Host "🎯 Build Complete! Ready to Deploy" -ForegroundColor Green
Write-Host ""
Write-Host "Choose next action:" -ForegroundColor Cyan
Write-Host "1. Run Backend (Port 8080)" -ForegroundColor Yellow
Write-Host "2. Run Frontend (Port 3000)" -ForegroundColor Yellow
Write-Host "3. Run Both (requires 2 terminal windows)" -ForegroundColor Yellow
Write-Host "4. Skip and exit" -ForegroundColor Yellow
Write-Host ""

$choice = Read-Host "Enter your choice (1-4)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "🚀 Starting Backend Server..." -ForegroundColor Cyan
        Write-Host "API will be available at: http://localhost:8080" -ForegroundColor Green
        Write-Host "Chatbot endpoint: http://localhost:8080/api/chatbot/chat" -ForegroundColor Green
        Write-Host ""
        java -jar $jarFile.FullName
    }
    "2" {
        Write-Host ""
        Write-Host "🚀 Starting Frontend Development Server..." -ForegroundColor Cyan
        Set-Location "frontend"
        Write-Host "Frontend will be available at: http://localhost:3000" -ForegroundColor Green
        Write-Host ""
        npm run dev
    }
    "3" {
        Write-Host ""
        Write-Host "📌 To run both servers:" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Terminal 1 - Backend:" -ForegroundColor Yellow
        Write-Host "  cd c:\Users\Hamdaan\Documents\maam" -ForegroundColor Green
        Write-Host "  `$env:GOOGLE_AI_API_KEY='AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE'" -ForegroundColor Green
        Write-Host "  java -jar target/$($jarFile.Name)" -ForegroundColor Green
        Write-Host ""
        Write-Host "Terminal 2 - Frontend:" -ForegroundColor Yellow
        Write-Host "  cd c:\Users\Hamdaan\Documents\maam\frontend" -ForegroundColor Green
        Write-Host "  npm run dev" -ForegroundColor Green
        Write-Host ""
        Write-Host "Then open: http://localhost:3000" -ForegroundColor Cyan
        Write-Host ""
        pause
    }
    "4" {
        Write-Host "Exiting..." -ForegroundColor Yellow
        exit 0
    }
    default {
        Write-Host "Invalid choice. Exiting..." -ForegroundColor Red
        exit 1
    }
}
