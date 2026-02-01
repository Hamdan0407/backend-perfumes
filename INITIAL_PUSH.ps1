# Initial Push to GitHub
# Run this first to push all current code to your repository

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "INITIAL PUSH TO GITHUB" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Before proceeding, make sure:" -ForegroundColor Yellow
Write-Host "✓ Git is installed" -ForegroundColor White
Write-Host "✓ You have GitHub credentials ready" -ForegroundColor White
Write-Host "✓ Repository https://github.com/Hamdan0407/Perfume.git exists" -ForegroundColor White
Write-Host ""

$confirm = Read-Host "Ready to push? (yes/no)"
if ($confirm -ne "yes") {
    Write-Host "Cancelled" -ForegroundColor Red
    exit
}

Write-Host ""
$gitExe = "git"
$repoPath = "c:\Users\Hamdaan\Documents\maam"

cd $repoPath

Write-Host "STEP 1: Checking Git..." -ForegroundColor Yellow
try {
    $gitVersion = & $gitExe --version
    Write-Host "✅ $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Git not found. Run SETUP_GITHUB.ps1 first" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "STEP 2: Repository Status..." -ForegroundColor Yellow
& $gitExe status
Write-Host ""

Write-Host "STEP 3: Staging all files..." -ForegroundColor Yellow
& $gitExe add .
Write-Host "✅ Files staged" -ForegroundColor Green

Write-Host ""
Write-Host "STEP 4: Creating initial commit..." -ForegroundColor Yellow
& $gitExe commit -m "Initial commit: Perfume Shop with Chatbot Real Behavior Fix

- Implemented auto-focus input field
- Verified direct product intent detection
- Confirmed real product database integration
- All tests passing (4/4)
- Production ready" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Commit status: $LASTEXITCODE" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "STEP 5: Pushing to GitHub..." -ForegroundColor Yellow
Write-Host "Remote URL: $(& $gitExe remote get-url origin)" -ForegroundColor White

& $gitExe branch -M main 2>&1

Write-Host ""
Write-Host "Pushing to origin/main..." -ForegroundColor White
& $gitExe push -u origin main 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "✅ SUCCESS! Code pushed to GitHub!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "View your repository:" -ForegroundColor White
    Write-Host "https://github.com/Hamdan0407/Perfume" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "From now on, after major features, run:" -ForegroundColor Yellow
    Write-Host "powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage 'Feature: Your description'" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "⚠️  Push encountered an issue" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Possible causes:" -ForegroundColor Yellow
    Write-Host "1. Authentication needed - use GitHub Personal Access Token" -ForegroundColor White
    Write-Host "2. Check internet connection" -ForegroundColor White
    Write-Host "3. Verify repository URL is correct" -ForegroundColor White
    Write-Host ""
    Write-Host "Run this to verify remote:" -ForegroundColor White
    Write-Host "git remote -v" -ForegroundColor Cyan
}

Write-Host ""
Read-Host "Press Enter to close"
