# PowerShell script to set up Gemini API key for chatbot

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Chatbot AI Setup - Gemini API Key" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check if API key is already set
if ($env:GEMINI_API_KEY) {
    Write-Host "✓ Gemini API key is already set!" -ForegroundColor Green
    Write-Host "Current key: $($env:GEMINI_API_KEY.Substring(0, 10))..." -ForegroundColor Gray
    Write-Host ""
    $overwrite = Read-Host "Do you want to update it? (y/n)"
    if ($overwrite -ne "y") {
        Write-Host "Keeping existing API key." -ForegroundColor Yellow
        exit 0
    }
}

Write-Host "To get your FREE Gemini API key:" -ForegroundColor Yellow
Write-Host "1. Visit: https://makersuite.google.com/app/apikey" -ForegroundColor White
Write-Host "2. Click 'Create API Key'" -ForegroundColor White
Write-Host "3. Copy the key (starts with 'AIza...')" -ForegroundColor White
Write-Host ""

# Prompt for API key
$apiKey = Read-Host "Enter your Gemini API key"

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Host "✗ No API key provided. Exiting." -ForegroundColor Red
    exit 1
}

# Validate API key format
if (-not $apiKey.StartsWith("AIza")) {
    Write-Host "⚠ Warning: API key doesn't start with 'AIza'. Are you sure this is correct?" -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        Write-Host "Setup cancelled." -ForegroundColor Red
        exit 1
    }
}

# Set environment variable for current session
$env:GEMINI_API_KEY = $apiKey
Write-Host "✓ API key set for current session!" -ForegroundColor Green

# Ask if user wants to save to .env file
Write-Host ""
$saveToFile = Read-Host "Save to .env file for persistence? (y/n)"

if ($saveToFile -eq "y") {
    $envFilePath = ".env"
    
    # Read existing .env if it exists
    $envContent = @()
    if (Test-Path $envFilePath) {
        $envContent = Get-Content $envFilePath
    }
    
    # Remove existing GEMINI_API_KEY line
    $envContent = $envContent | Where-Object { $_ -notmatch "^GEMINI_API_KEY=" }
    
    # Add new API key
    $envContent += "GEMINI_API_KEY=$apiKey"
    $envContent += "GEMINI_ENABLED=true"
    
    # Write back to file
    $envContent | Set-Content $envFilePath
    
    Write-Host "✓ API key saved to .env file!" -ForegroundColor Green
}

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Start backend: mvn spring-boot:run" -ForegroundColor White
Write-Host "2. Start frontend: cd frontend && npm run dev" -ForegroundColor White
Write-Host "3. Open chatbot and test AI responses!" -ForegroundColor White
Write-Host ""
Write-Host "Test queries:" -ForegroundColor Yellow
Write-Host "- 'I need a perfume for a romantic date'" -ForegroundColor Gray
Write-Host "- 'What's the price of Dior Sauvage?'" -ForegroundColor Gray
Write-Host "- 'Recommend something fresh and professional'" -ForegroundColor Gray
Write-Host ""
