# GitHub Setup and Auto-Push Script
# This script installs Git and sets up automatic GitHub pushes

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "GITHUB SETUP - Auto Push to Your Repo" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check and Install Git
Write-Host "STEP 1: Checking Git Installation..." -ForegroundColor Yellow

$gitPaths = @(
    'C:\Program Files\Git\bin\git.exe',
    'C:\Program Files (x86)\Git\bin\git.exe'
)

$gitFound = $false
foreach ($path in $gitPaths) {
    if (Test-Path $path) {
        Write-Host "✅ Git found at: $path" -ForegroundColor Green
        $gitFound = $true
        break
    }
}

if (-not $gitFound) {
    Write-Host "❌ Git not installed. Downloading Git installer..." -ForegroundColor Red
    Write-Host ""
    Write-Host "Please follow these steps:" -ForegroundColor Yellow
    Write-Host "1. Download: https://git-scm.com/download/win" -ForegroundColor White
    Write-Host "2. Run the installer" -ForegroundColor White
    Write-Host "3. Choose default options during installation" -ForegroundColor White
    Write-Host "4. Run this script again" -ForegroundColor White
    Write-Host ""
    Read-Host "Press Enter once Git is installed"
    
    # Try to open the download page
    Start-Process "https://git-scm.com/download/win"
    exit
}

Write-Host ""
Write-Host "STEP 2: Initializing Git Repository..." -ForegroundColor Yellow

# Set up Git path
$gitExe = "git"

# Check if already a git repo
cd c:\Users\Hamdaan\Documents\maam
if (Test-Path ".git") {
    Write-Host "✅ Git repository already exists" -ForegroundColor Green
} else {
    Write-Host "Initializing new git repository..." -ForegroundColor White
    & $gitExe init
    Write-Host "✅ Git repository initialized" -ForegroundColor Green
}

Write-Host ""
Write-Host "STEP 3: Configuring Git..." -ForegroundColor Yellow

# Configure git user (if not already configured)
$gitUserName = & $gitExe config user.name 2>$null
$gitUserEmail = & $gitExe config user.email 2>$null

if ([string]::IsNullOrEmpty($gitUserName)) {
    Write-Host "Setting up Git user configuration..." -ForegroundColor White
    & $gitExe config user.name "Hamdaan Developer"
    & $gitExe config user.email "hamdan0407@example.com"
    Write-Host "✅ Git user configured" -ForegroundColor Green
} else {
    Write-Host "✅ Git user already configured: $gitUserName ($gitUserEmail)" -ForegroundColor Green
}

Write-Host ""
Write-Host "STEP 4: Adding Remote Repository..." -ForegroundColor Yellow

$repoUrl = "https://github.com/Hamdan0407/Perfume.git"
$remoteExists = & $gitExe remote -v 2>$null | Select-String "origin"

if ($remoteExists) {
    Write-Host "✅ Remote repository already configured" -ForegroundColor Green
    Write-Host "Remote: $(& $gitExe remote get-url origin)" -ForegroundColor White
} else {
    Write-Host "Adding remote: $repoUrl" -ForegroundColor White
    & $gitExe remote add origin $repoUrl
    Write-Host "✅ Remote repository added" -ForegroundColor Green
}

Write-Host ""
Write-Host "STEP 5: Creating .gitignore..." -ForegroundColor Yellow

$gitignorePath = "c:\Users\Hamdaan\Documents\maam\.gitignore"
if (-not (Test-Path $gitignorePath)) {
    @"
# Build directories
target/
dist/
build/
node_modules/

# IDE files
.vscode/
.idea/
*.swp
*.swo
*~

# OS files
.DS_Store
Thumbs.db

# Log files
*.log
logs/

# Environment files
.env
.env.local

# Temporary files
temp/
tmp/
*.tmp

# Maven
.m2/

# Node
npm-debug.log
yarn-error.log

# Python
__pycache__/
*.pyc
"@ | Out-File -FilePath $gitignorePath -Encoding UTF8
    Write-Host "✅ .gitignore created" -ForegroundColor Green
} else {
    Write-Host "✅ .gitignore already exists" -ForegroundColor Green
}

Write-Host ""
Write-Host "STEP 6: Creating Auto-Push Script..." -ForegroundColor Yellow

$autoPushScript = @"
# Auto-Push to GitHub Script
# Run this after completing major features

param(
    [string]`$CommitMessage = "Feature: Latest updates"
)

`$gitExe = "git"
`$repoPath = "c:\Users\Hamdaan\Documents\maam"

cd `$repoPath

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AUTO-PUSHING TO GITHUB" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Repository: https://github.com/Hamdan0407/Perfume.git" -ForegroundColor White
Write-Host "Message: `$CommitMessage" -ForegroundColor White
Write-Host ""

# Stage all changes
Write-Host "Staging changes..." -ForegroundColor Yellow
& `$gitExe add . 2>&1 | ForEach-Object { Write-Host "  `$_" -ForegroundColor Gray }

# Get status
`$status = & `$gitExe status --porcelain
if ([string]::IsNullOrEmpty(`$status)) {
    Write-Host "❌ No changes to commit" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Changes staged:" -ForegroundColor Green
`$status | ForEach-Object { Write-Host "  `$_" -ForegroundColor White }

# Create commit
Write-Host ""
Write-Host "Creating commit..." -ForegroundColor Yellow
& `$gitExe commit -m "`$CommitMessage" 2>&1 | ForEach-Object { Write-Host "  `$_" -ForegroundColor Gray }
if (`$LASTEXITCODE -ne 0) {
    Write-Host "❌ Commit failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Commit created" -ForegroundColor Green

# Push to GitHub
Write-Host ""
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
& `$gitExe push -u origin main 2>&1 | ForEach-Object { Write-Host "  `$_" -ForegroundColor Gray }
if (`$LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "⚠️  Push encountered an issue. This might be because:" -ForegroundColor Yellow
    Write-Host "  1. Branch doesn't exist on GitHub yet" -ForegroundColor White
    Write-Host "  2. Authentication is needed" -ForegroundColor White
    Write-Host "  3. Network connection issue" -ForegroundColor White
    exit 1
}

Write-Host "✅ Successfully pushed to GitHub!" -ForegroundColor Green
Write-Host ""
Write-Host "View your repo: https://github.com/Hamdan0407/Perfume" -ForegroundColor Cyan
"@

$autoPushPath = "c:\Users\Hamdaan\Documents\maam\auto-push.ps1"
$autoPushScript | Out-File -FilePath $autoPushPath -Encoding UTF8
Write-Host "✅ Auto-push script created: auto-push.ps1" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SETUP COMPLETE!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. First time? Run:" -ForegroundColor White
Write-Host "   powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\INITIAL_PUSH.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. After features? Run:" -ForegroundColor White
Write-Host "   powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage 'Feature: Your description'" -ForegroundColor Cyan
Write-Host ""
Write-Host "Example:" -ForegroundColor White
Write-Host "   powershell -ExecutionPolicy Bypass c:\Users\Hamdaan\Documents\maam\auto-push.ps1 -CommitMessage 'Feature: Chatbot real behavior fix'" -ForegroundColor Cyan
Write-Host ""

Read-Host "Press Enter to continue"
