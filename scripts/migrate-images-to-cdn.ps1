# PowerShell CDN Image Migration Script
# This script helps migrate local images to CDN (Cloudflare R2 or AWS S3)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "CDN Image Migration Script" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$PROVIDER = Read-Host "Enter CDN provider (r2/s3)"
$BUCKET_NAME = Read-Host "Enter bucket name"
$LOCAL_DIR = Read-Host "Enter local images directory [./uploads]"
if ([string]::IsNullOrWhiteSpace($LOCAL_DIR)) { $LOCAL_DIR = "./uploads" }

if ($PROVIDER -eq "r2") {
    $ENDPOINT_URL = Read-Host "Enter R2 endpoint URL"
    $ACCESS_KEY = Read-Host "Enter R2 access key"
    $SECRET_KEY = Read-Host "Enter R2 secret key" -AsSecureString
    $SECRET_KEY_PLAIN = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($SECRET_KEY))
    
    # Configure AWS CLI for R2
    aws configure set aws_access_key_id $ACCESS_KEY --profile r2
    aws configure set aws_secret_access_key $SECRET_KEY_PLAIN --profile r2
    aws configure set region auto --profile r2
    
    $SYNC_CMD = "aws s3 sync $LOCAL_DIR s3://$BUCKET_NAME --endpoint-url $ENDPOINT_URL --profile r2 --acl public-read"
    
}
elseif ($PROVIDER -eq "s3") {
    $AWS_REGION = Read-Host "Enter AWS region [us-east-1]"
    if ([string]::IsNullOrWhiteSpace($AWS_REGION)) { $AWS_REGION = "us-east-1" }
    
    Write-Host "Make sure AWS CLI is configured with your credentials" -ForegroundColor Yellow
    Write-Host "Run: aws configure" -ForegroundColor Yellow
    Write-Host ""
    
    $SYNC_CMD = "aws s3 sync $LOCAL_DIR s3://$BUCKET_NAME --region $AWS_REGION --acl public-read"
}
else {
    Write-Host "Invalid provider. Use 'r2' or 's3'" -ForegroundColor Red
    exit 1
}

# Show what will be uploaded
Write-Host ""
Write-Host "Files to upload:" -ForegroundColor Yellow
Get-ChildItem -Path $LOCAL_DIR -File -Recurse | Select-Object -First 10 | ForEach-Object { Write-Host $_.FullName }
Write-Host "..."
Write-Host ""

$fileCount = (Get-ChildItem -Path $LOCAL_DIR -File -Recurse).Count
$CONFIRM = Read-Host "Upload $fileCount files to $BUCKET_NAME? (y/n)"

if ($CONFIRM -ne "y") {
    Write-Host "Upload cancelled" -ForegroundColor Yellow
    exit 0
}

# Upload
Write-Host ""
Write-Host "Uploading images..." -ForegroundColor Cyan
Invoke-Expression $SYNC_CMD

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Upload complete!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Verify images are accessible in your CDN dashboard"
    Write-Host "2. Update .env.production with CDN_ENABLED=true"
    Write-Host "3. Update CDN_BASE_URL with your CDN URL"
    Write-Host "4. Restart your application"
}
else {
    Write-Host ""
    Write-Host "❌ Upload failed. Check your credentials and try again." -ForegroundColor Red
    exit 1
}
