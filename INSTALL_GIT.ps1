# Install Git Automatically
# This script downloads and installs Git for Windows

Write-Host "╔════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        INSTALLING GIT FOR WINDOWS                 ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$installerUrl = "https://github.com/git-for-windows/git/releases/download/v2.45.0.windows.1/Git-2.45.0-64-bit.exe"
$installerPath = "$env:TEMP\GitInstaller.exe"

Write-Host "Downloading Git installer..." -ForegroundColor Yellow
Write-Host "URL: $installerUrl" -ForegroundColor Gray
Write-Host ""

try {
    # Download Git installer
    $progressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $installerUrl -OutFile $installerPath -UseBasicParsing
    Write-Host "✅ Download complete" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "Installing Git..." -ForegroundColor Yellow
    Write-Host "(This may take 1-2 minutes)" -ForegroundColor Gray
    Write-Host ""
    
    # Run installer with silent options
    & $installerPath /SILENT /NORESTART
    
    # Wait for installation
    Start-Sleep -Seconds 5
    
    # Verify installation
    $gitPath = "C:\Program Files\Git\bin\git.exe"
    if (Test-Path $gitPath) {
        Write-Host "✅ Git installed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Yellow
        Write-Host "1. Close this window" -ForegroundColor White
        Write-Host "2. Open a NEW PowerShell window (important!)" -ForegroundColor White
        Write-Host "3. Run the setup script again:" -ForegroundColor White
        Write-Host "   powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\SETUP_GITHUB.ps1" -ForegroundColor Cyan
        Write-Host ""
        Read-Host "Press Enter to exit"
    } else {
        Write-Host "⚠️  Git installation verification failed" -ForegroundColor Yellow
        Write-Host "Try downloading manually from: https://git-scm.com/download/win" -ForegroundColor White
    }
    
    # Cleanup
    Remove-Item $installerPath -Force -ErrorAction SilentlyContinue
    
} catch {
    Write-Host "❌ Download failed. Please download manually:" -ForegroundColor Red
    Write-Host "https://git-scm.com/download/win" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Error: $_" -ForegroundColor Red
    Read-Host "Press Enter to exit"
}
