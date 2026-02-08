#!/bin/bash

# CDN Image Migration Script
# This script helps migrate local images to CDN (Cloudflare R2 or AWS S3)

echo "========================================="
echo "CDN Image Migration Script"
echo "========================================="
echo ""

# Configuration
read -p "Enter CDN provider (r2/s3): " PROVIDER
read -p "Enter bucket name: " BUCKET_NAME
read -p "Enter local images directory [./uploads]: " LOCAL_DIR
LOCAL_DIR=${LOCAL_DIR:-./uploads}

if [ "$PROVIDER" = "r2" ]; then
    read -p "Enter R2 endpoint URL: " ENDPOINT_URL
    read -p "Enter R2 access key: " ACCESS_KEY
    read -s -p "Enter R2 secret key: " SECRET_KEY
    echo ""
    
    # Configure AWS CLI for R2
    aws configure set aws_access_key_id "$ACCESS_KEY" --profile r2
    aws configure set aws_secret_access_key "$SECRET_KEY" --profile r2
    aws configure set region auto --profile r2
    
    SYNC_CMD="aws s3 sync $LOCAL_DIR s3://$BUCKET_NAME --endpoint-url $ENDPOINT_URL --profile r2 --acl public-read"
    
elif [ "$PROVIDER" = "s3" ]; then
    read -p "Enter AWS region [us-east-1]: " AWS_REGION
    AWS_REGION=${AWS_REGION:-us-east-1}
    
    echo "Make sure AWS CLI is configured with your credentials"
    echo "Run: aws configure"
    echo ""
    
    SYNC_CMD="aws s3 sync $LOCAL_DIR s3://$BUCKET_NAME --region $AWS_REGION --acl public-read"
else
    echo "Invalid provider. Use 'r2' or 's3'"
    exit 1
fi

# Show what will be uploaded
echo ""
echo "Files to upload:"
find "$LOCAL_DIR" -type f | head -10
echo "..."
echo ""

# Confirm
read -p "Upload $(find "$LOCAL_DIR" -type f | wc -l) files to $BUCKET_NAME? (y/n): " CONFIRM

if [ "$CONFIRM" != "y" ]; then
    echo "Upload cancelled"
    exit 0
fi

# Upload
echo ""
echo "Uploading images..."
eval $SYNC_CMD

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Upload complete!"
    echo ""
    echo "Next steps:"
    echo "1. Verify images are accessible in your CDN dashboard"
    echo "2. Update .env.production with CDN_ENABLED=true"
    echo "3. Update CDN_BASE_URL with your CDN URL"
    echo "4. Restart your application"
else
    echo ""
    echo "❌ Upload failed. Check your credentials and try again."
    exit 1
fi
