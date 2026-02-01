# Quick Push to GitHub
# Use this after completing major features

param(
    [string]$CommitMessage = "Feature: Updates"
)

$gitExe = "git"
$repoPath = "c:\Users\Hamdaan\Documents\maam"

cd $repoPath

Write-Host "╔════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║          PUSHING TO GITHUB                         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "Repository: https://github.com/Hamdan0407/Perfume" -ForegroundColor White
Write-Host "Commit Message: $CommitMessage" -ForegroundColor White
Write-Host ""

# Check git
try {
    & $gitExe --version > $null 2>&1
} catch {
    Write-Host "❌ Git not installed. Run SETUP_GITHUB.ps1" -ForegroundColor Red
    exit 1
}

# Stage changes
Write-Host "📦 Staging changes..." -ForegroundColor Yellow
& $gitExe add .

# Check if there are changes
$status = & $gitExe status --porcelain
if ([string]::IsNullOrEmpty($status)) {
    Write-Host "ℹ️  No changes to commit" -ForegroundColor Yellow
    exit 0
}

Write-Host "✅ Changes detected:" -ForegroundColor Green
$status | ForEach-Object { Write-Host "   $_" -ForegroundColor White }

# Commit
Write-Host ""
Write-Host "💾 Committing..." -ForegroundColor Yellow
& $gitExe commit -m "$CommitMessage" 2>&1 | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }

# Push
Write-Host ""
Write-Host "🚀 Pushing to GitHub..." -ForegroundColor Yellow
& $gitExe push 2>&1 | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ SUCCESS! Pushed to GitHub!" -ForegroundColor Green
    Write-Host "📍 https://github.com/Hamdan0407/Perfume/commits" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "⚠️  Push failed. Check your:" -ForegroundColor Yellow
    Write-Host "   • Internet connection" -ForegroundColor White
    Write-Host "   • GitHub credentials" -ForegroundColor White
    Write-Host "   • Repository URL" -ForegroundColor White
}

Write-Host ""
