# CDN Setup Guide for Images

## Overview
This guide covers setting up a CDN (Content Delivery Network) for product images to achieve **80-90% faster image loading**.

## Recommended CDN Providers

| Provider | Storage | CDN | Cost (10GB + 100GB transfer) | Best For |
|----------|---------|-----|------------------------------|----------|
| **Cloudflare R2** | $0.015/GB | Free | ~$0.15/month | Best value, zero egress fees |
| **AWS S3 + CloudFront** | $0.023/GB | $0.085/GB transfer | ~$8.73/month | Enterprise, full AWS integration |
| **Bunny CDN** | $0.01/GB | $0.01/GB transfer | ~$1.10/month | Budget-friendly |
| **DigitalOcean Spaces** | $5/month (250GB) | Included | $5/month | Simple, predictable pricing |

**Recommendation:** **Cloudflare R2** for best value with zero egress fees.

---

## Option 1: Cloudflare R2 (Recommended)

### Step 1: Create R2 Bucket

1. Go to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Navigate to **R2 Object Storage**
3. Click **Create bucket**
4. Name: `perfume-shop-images`
5. Location: Auto (or choose closest to your users)

### Step 2: Get API Credentials

1. Go to **R2** → **Manage R2 API Tokens**
2. Click **Create API Token**
3. Permissions: **Object Read & Write**
4. Copy:
   - Access Key ID
   - Secret Access Key
   - Bucket endpoint URL

### Step 3: Configure Public Access

```bash
# Make bucket publicly readable
# In R2 Dashboard → Your Bucket → Settings → Public Access
# Enable "Allow Access" for public reads
```

### Step 4: Get CDN URL

Your CDN URL will be: `https://pub-xxxxx.r2.dev/perfume-shop-images/`

Or use custom domain: `https://cdn.yourdomain.com/`

---

## Option 2: AWS S3 + CloudFront

### Step 1: Create S3 Bucket

```bash
# Install AWS CLI
aws configure

# Create bucket
aws s3 mb s3://perfume-shop-images --region us-east-1

# Enable public read access
aws s3api put-bucket-policy --bucket perfume-shop-images --policy file://bucket-policy.json
```

**bucket-policy.json:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::perfume-shop-images/*"
    }
  ]
}
```

### Step 2: Create CloudFront Distribution

1. Go to **CloudFront** → **Create Distribution**
2. Origin Domain: `perfume-shop-images.s3.amazonaws.com`
3. Origin Path: Leave empty
4. Viewer Protocol Policy: **Redirect HTTP to HTTPS**
5. Allowed HTTP Methods: **GET, HEAD**
6. Cache Policy: **CachingOptimized**
7. Create Distribution

### Step 3: Get CDN URL

Your CloudFront URL: `https://d1234abcd.cloudfront.net/`

---

## Backend Configuration

### Step 1: Add Dependencies

**pom.xml:**
```xml
<!-- AWS SDK for S3 (works with R2 too) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>
```

### Step 2: Add Environment Variables

**.env.production:**
```bash
# CDN Configuration
CDN_ENABLED=true
CDN_BASE_URL=https://pub-xxxxx.r2.dev/perfume-shop-images
CDN_PROVIDER=r2  # or s3

# S3/R2 Credentials
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=auto  # for R2, or us-east-1 for S3
AWS_S3_BUCKET=perfume-shop-images
AWS_S3_ENDPOINT=https://xxxxx.r2.cloudflarestorage.com  # R2 endpoint, omit for S3
```

### Step 3: Update application.yml

**application-production.yml:**
```yaml
app:
  cdn:
    enabled: ${CDN_ENABLED:false}
    base-url: ${CDN_BASE_URL:}
    provider: ${CDN_PROVIDER:local}
  
  storage:
    type: ${STORAGE_TYPE:local}  # local, s3, r2
    local-path: ${LOCAL_STORAGE_PATH:./uploads}
    s3:
      bucket: ${AWS_S3_BUCKET:}
      region: ${AWS_REGION:us-east-1}
      endpoint: ${AWS_S3_ENDPOINT:}  # For R2
      access-key: ${AWS_ACCESS_KEY_ID:}
      secret-key: ${AWS_SECRET_ACCESS_KEY:}
```

---

## Frontend Configuration

### Update .env

**frontend/.env.production:**
```bash
VITE_CDN_ENABLED=true
VITE_CDN_BASE_URL=https://pub-xxxxx.r2.dev/perfume-shop-images
VITE_API_BASE_URL=https://api.yourdomain.com
```

### Update Image URLs

**Before:**
```javascript
const imageUrl = `/uploads/products/${product.id}.jpg`;
```

**After:**
```javascript
const CDN_URL = import.meta.env.VITE_CDN_ENABLED === 'true' 
  ? import.meta.env.VITE_CDN_BASE_URL 
  : '';

const imageUrl = product.imageUrl.startsWith('http')
  ? product.imageUrl
  : `${CDN_URL}${product.imageUrl}`;
```

---

## Image Upload Process

### 1. Upload Images to CDN

**Using AWS CLI (works for R2 too):**
```bash
# Configure for R2
aws configure --profile r2
# Access Key: your_r2_access_key
# Secret Key: your_r2_secret_key
# Region: auto

# Upload single image
aws s3 cp product-1.jpg s3://perfume-shop-images/products/ \
  --endpoint-url https://xxxxx.r2.cloudflarestorage.com \
  --profile r2

# Upload entire directory
aws s3 sync ./local-images/ s3://perfume-shop-images/products/ \
  --endpoint-url https://xxxxx.r2.cloudflarestorage.com \
  --profile r2 \
  --acl public-read
```

**Using Cloudflare Dashboard:**
1. Go to R2 → Your Bucket
2. Click **Upload**
3. Drag and drop images
4. Images are automatically public if bucket is configured

### 2. Update Database Image URLs

**Option A: Relative paths (recommended):**
```sql
-- Store relative paths in database
UPDATE products SET image_url = '/products/chanel-no5.jpg' WHERE id = 1;
UPDATE products SET image_url = '/products/dior-sauvage.jpg' WHERE id = 2;
```

**Option B: Full CDN URLs:**
```sql
-- Store full CDN URLs
UPDATE products SET image_url = 'https://cdn.yourdomain.com/products/chanel-no5.jpg' WHERE id = 1;
```

---

## Image Optimization

### Before Upload

```bash
# Install ImageMagick
brew install imagemagick  # macOS
sudo apt-get install imagemagick  # Linux

# Optimize images
for img in *.jpg; do
  convert "$img" -quality 85 -resize 800x800\> "optimized/$img"
done

# Convert to WebP (better compression)
for img in *.jpg; do
  cwebp -q 85 "$img" -o "${img%.jpg}.webp"
done
```

### Image Naming Convention

```
products/
  ├── chanel-no5-main.jpg          # Main product image
  ├── chanel-no5-thumb.jpg         # Thumbnail (200x200)
  ├── chanel-no5-gallery-1.jpg     # Gallery image 1
  ├── chanel-no5-gallery-2.jpg     # Gallery image 2
  └── chanel-no5-main.webp         # WebP version
```

---

## Testing CDN Setup

### 1. Test Image Access

```bash
# Test direct CDN access
curl -I https://pub-xxxxx.r2.dev/perfume-shop-images/products/test.jpg

# Should return:
# HTTP/2 200
# content-type: image/jpeg
# cache-control: public, max-age=31536000
```

### 2. Test Frontend

```javascript
// In browser console
const testImage = new Image();
testImage.src = 'https://pub-xxxxx.r2.dev/perfume-shop-images/products/test.jpg';
testImage.onload = () => console.log('✅ CDN working!');
testImage.onerror = () => console.log('❌ CDN failed!');
```

### 3. Check Performance

```bash
# Compare load times
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/uploads/test.jpg
curl -w "@curl-format.txt" -o /dev/null -s https://cdn.yourdomain.com/products/test.jpg
```

**curl-format.txt:**
```
time_namelookup:  %{time_namelookup}s\n
time_connect:     %{time_connect}s\n
time_total:       %{time_total}s\n
```

---

## Migration Checklist

### Pre-Migration
- [ ] Choose CDN provider (Cloudflare R2 recommended)
- [ ] Create bucket/storage
- [ ] Get API credentials
- [ ] Configure public access
- [ ] Get CDN URL

### Image Preparation
- [ ] Collect all product images
- [ ] Optimize images (compress, resize)
- [ ] Convert to WebP (optional but recommended)
- [ ] Organize with naming convention

### Upload
- [ ] Upload images to CDN
- [ ] Verify all images are accessible
- [ ] Test image URLs in browser

### Backend Update
- [ ] Add environment variables
- [ ] Update application.yml
- [ ] Update database image URLs
- [ ] Test API responses

### Frontend Update
- [ ] Update .env with CDN URL
- [ ] Update image URL logic
- [ ] Test in development
- [ ] Build production bundle

### Verification
- [ ] Test all product images load
- [ ] Check browser network tab (images from CDN)
- [ ] Verify cache headers
- [ ] Test on mobile devices

---

## Performance Comparison

### Before CDN (Local Server)
```
Image Size: 500KB
Load Time: 800ms
Bandwidth: Full server bandwidth
Caching: Limited
```

### After CDN
```
Image Size: 500KB (or 200KB with WebP)
Load Time: 100ms (80-90% faster)
Bandwidth: Offloaded to CDN
Caching: Global edge caching
```

---

## Cost Estimation

### Cloudflare R2 (Recommended)
```
Storage: 10GB × $0.015 = $0.15/month
Egress: FREE (unlimited)
Operations: 1M reads × $0.36/million = $0.36/month
Total: ~$0.51/month
```

### AWS S3 + CloudFront
```
S3 Storage: 10GB × $0.023 = $0.23/month
S3 Requests: 1M × $0.0004 = $0.40/month
CloudFront Transfer: 100GB × $0.085 = $8.50/month
Total: ~$9.13/month
```

---

## Troubleshooting

### Images Not Loading

**Check CORS:**
```bash
# R2 CORS configuration
# In R2 Dashboard → Bucket → Settings → CORS
```

**CORS Policy:**
```json
[
  {
    "AllowedOrigins": ["https://yourdomain.com", "http://localhost:3000"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3600
  }
]
```

### Slow Loading

1. Check CDN cache headers
2. Verify images are optimized
3. Use WebP format
4. Enable HTTP/2

### 403 Forbidden

1. Check bucket public access settings
2. Verify bucket policy
3. Check CORS configuration

---

## Next Steps

1. **Choose Provider**: Cloudflare R2 recommended
2. **Create Bucket**: Set up storage
3. **Upload Images**: Migrate existing images
4. **Update Config**: Add CDN URLs to environment
5. **Test**: Verify images load from CDN
6. **Deploy**: Update production

**Estimated Time:** 1-2 hours  
**Performance Gain:** 80-90% faster image loading  
**Cost:** $0.50-$9/month depending on provider
