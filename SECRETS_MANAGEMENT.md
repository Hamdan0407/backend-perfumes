# Secrets Management Guide

Secure handling of sensitive data in production.

## Secret Types & Storage

### Application Secrets (Never commit to Git)

**Type:** Credentials, API keys, tokens
**Storage:** Environment variables or secrets manager
**Rotation:** Every 90 days

| Secret | Purpose | Rotation | Storage |
|--------|---------|----------|---------|
| JWT_SECRET | Token signing | 90 days | .env.production |
| DATABASE_PASSWORD | Database access | 90 days | .env.production |
| MAIL_PASSWORD | Email sending | 90 days | .env.production |
| STRIPE_API_KEY | Payment processing | As needed | .env.production |
| RAZORPAY_KEY_SECRET | Payment processing | As needed | .env.production |
| STRIPE_WEBHOOK_SECRET | Webhook verification | As needed | .env.production |

### .gitignore Configuration

```
# Never commit these files
.env
.env.production
.env.local
.env.*.local
*.key
*.pem
secrets/
credentials/
*.backup
*.sql
```

## Secret Generation

### JWT Secret (256-bit)

```bash
# Using OpenSSL (Recommended)
openssl rand -base64 32

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Result: 32-character base64 string
# Example: aB3dE5fG7hJ9kL1mN3pQ5rS7tU9vW1xY
```

### Database Password (16+ characters)

```bash
# Generate strong password
openssl rand -base64 16

# Or use password manager
# Requirements:
# - Min 16 characters
# - Uppercase, lowercase, numbers, special characters
# - No words from dictionary
```

### API Keys

**Stripe:**
- Test: `sk_test_...` → `pk_test_...`
- Live: `sk_live_...` → `pk_live_...`
- Get from: https://dashboard.stripe.com/apikeys

**Razorpay:**
- Test: `rzp_test_...`
- Live: `rzp_live_...`
- Get from: https://dashboard.razorpay.com/

### Webhook Secrets

Generated automatically by payment providers:
- Stripe: From webhook endpoint dashboard
- Razorpay: From settings page

## Secrets Storage Solutions

### Option 1: Environment Variables (Simple)

**Advantages:**
- No additional tools needed
- Works with Docker and systemd
- Cloud-ready

**Setup:**
```bash
# Create .env.production (git-ignored)
DATABASE_URL=jdbc:mysql://...
JWT_SECRET=...

# Load in systemd
EnvironmentFile=/opt/perfume-shop/.env.production

# Load in Docker
env_file:
  - .env.production
```

### Option 2: Cloud Secrets Manager (Recommended)

#### AWS Secrets Manager
```bash
# Store secret
aws secretsmanager create-secret \
  --name perfume-shop/prod/jwt-secret \
  --secret-string "your-secret-key"

# Retrieve secret
aws secretsmanager get-secret-value \
  --secret-id perfume-shop/prod/jwt-secret

# Rotate automatically
aws secretsmanager rotate-secret \
  --secret-id perfume-shop/prod/jwt-secret \
  --rotation-rules AutomaticallyAfterDays=30
```

#### Google Cloud Secret Manager
```bash
# Store secret
gcloud secrets create perfume-shop-jwt-secret \
  --replication-policy="automatic" \
  --data-file=-

# Retrieve secret
gcloud secrets versions access latest \
  --secret="perfume-shop-jwt-secret"
```

#### Azure Key Vault
```bash
# Store secret
az keyvault secret set \
  --vault-name perfume-shop-vault \
  --name jwt-secret \
  --value "your-secret-key"

# Retrieve secret
az keyvault secret show \
  --vault-name perfume-shop-vault \
  --name jwt-secret
```

### Option 3: Hashicorp Vault (Enterprise)

```bash
# Initialize vault
vault server -config=vault.hcl

# Store secret
vault kv put secret/perfume-shop/prod \
  jwt_secret="..." \
  db_password="..."

# Retrieve secret
vault kv get secret/perfume-shop/prod

# Enable auto-rotation
vault write aws/roles/perfume-shop \
  credential_type=iam_user
```

## GitHub Actions Secrets

For CI/CD pipelines, store secrets in GitHub:

1. Go to Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Add secrets:
   ```
   DOCKERHUB_USERNAME=your-username
   DOCKERHUB_TOKEN=your-token
   DEPLOY_KEY=your-ssh-private-key
   DEPLOY_USER=ubuntu
   DEPLOY_HOST=your-server.com
   ```

## Secrets in Docker

### Using .env File
```bash
# Create .env.production (git-ignored)
DATABASE_PASSWORD=secure_password_here
JWT_SECRET=secure_key_here

# Reference in docker-compose.yml
environment:
  DATABASE_PASSWORD: ${DATABASE_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}

# Load file
docker-compose --env-file .env.production up
```

### Using Docker Secrets (Swarm)
```bash
# Create secret
echo "secure_password" | docker secret create db_password -

# Use in service
docker service create \
  --secret db_password \
  --env DATABASE_PASSWORD=/run/secrets/db_password \
  perfume-shop-api
```

### Using Environment Variables
```bash
# Export variables
export DATABASE_PASSWORD=secure_password
export JWT_SECRET=secure_key

# Start container
docker-compose up
```

## Security Best Practices

### 1. Never Log Secrets
```java
// ❌ WRONG
logger.info("Connecting with password: " + password);

// ✅ CORRECT
logger.info("Connecting to database");
```

### 2. Rotate Secrets Regularly
```bash
# Create rotation schedule
# Every 90 days for JWT_SECRET
# Every 30 days for DATABASE_PASSWORD
# As-needed for API keys
```

### 3. Use Different Secrets per Environment
```
Development:  sk_test_dev_key (Stripe test)
Staging:      sk_test_staging_key (Stripe test)
Production:   sk_live_prod_key (Stripe live)
```

### 4. Limit Secret Access
```bash
# Only specific users/services can access
chmod 600 .env.production
chown perfume:perfume /opt/perfume-shop/.env.production
```

### 5. Audit Secret Usage
```bash
# Log all secret access
grep "JWT_SECRET\|DATABASE_PASSWORD" /var/log/perfume-shop/*.log

# Monitor for unauthorized access
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=ResourceName,AttributeValue=perfume-shop-db-password
```

## Secret Rotation Procedure

### Step 1: Generate New Secret
```bash
# Generate new JWT secret
openssl rand -base64 32
# Result: newSecretKey123...
```

### Step 2: Update in Secrets Manager
```bash
# AWS
aws secretsmanager update-secret \
  --secret-id perfume-shop/prod/jwt-secret \
  --secret-string "newSecretKey123..."

# Or update .env.production
JWT_SECRET=newSecretKey123...
```

### Step 3: Update Application
```bash
# For running application without downtime:
# 1. Deploy new version with new secret
docker-compose pull
docker-compose up -d

# 2. Verify application started
docker-compose logs api | grep "Started"

# 3. Test API with new secret
curl http://localhost:8080/actuator/health
```

### Step 4: Invalidate Old Secret
```bash
# Database password: Update user
ALTER USER 'prod_user'@'localhost' IDENTIFIED BY 'newPassword123...';

# JWT: Old tokens will expire naturally
# For immediate effect, restart application
```

### Step 5: Audit Trail
```bash
# Log rotation
echo "2024-01-15: JWT_SECRET rotated by admin" >> /var/log/perfume-shop/rotation.log
echo "2024-01-15: DATABASE_PASSWORD rotated by admin" >> /var/log/perfume-shop/rotation.log
```

## Incident Response

### Compromised JWT Secret

1. **Immediate:** Revoke all active tokens
   ```sql
   UPDATE session SET revoked = true WHERE status = 'ACTIVE';
   ```

2. **Generate new secret**
   ```bash
   openssl rand -base64 32
   ```

3. **Update application**
   ```bash
   export JWT_SECRET=newKey123...
   docker-compose restart api
   ```

4. **Force user re-authentication**
   ```sql
   UPDATE users SET last_login = NOW() - INTERVAL 1 DAY;
   -- Users will need to log in again
   ```

5. **Review logs**
   ```bash
   grep "JWT\|Token\|Authentication" /var/log/perfume-shop/*.log | tail -100
   ```

### Compromised Database Password

1. **Immediate:** Block application traffic
   ```bash
   docker-compose pause api
   ```

2. **Reset database password**
   ```sql
   ALTER USER 'prod_user'@'localhost' IDENTIFIED BY 'newPassword123...';
   ```

3. **Update application config**
   ```bash
   export DATABASE_PASSWORD=newPassword123...
   docker-compose restart api
   ```

4. **Review access logs**
   ```bash
   mysql -u prod_user -p perfume_shop -e "SHOW PROCESSLIST;"
   ```

### Compromised API Key (Stripe/Razorpay)

1. **Immediate:** Revoke old key in provider dashboard

2. **Generate new key**
   - Stripe: https://dashboard.stripe.com/apikeys
   - Razorpay: https://dashboard.razorpay.com

3. **Update application**
   ```bash
   export STRIPE_API_KEY=sk_live_newkey...
   docker-compose restart api
   ```

4. **Monitor for fraudulent charges**
   - Check transaction logs
   - Alert monitoring service

## Compliance & Audit

### Secret Access Logging

```bash
# Log secret access
auditctl -w /opt/perfume-shop/.env.production \
         -p wa -k secret_changes

# View audit log
ausearch -k secret_changes
```

### Encryption at Rest

```bash
# Enable full-disk encryption
sudo apt install ecryptfs-utils

# Encrypt directory
ecryptfs-mount-private /opt/perfume-shop
```

### Encryption in Transit

```yaml
# Enable HTTPS for all communication
server:
  port: 8080
  ssl:
    key-store: /path/to/keystore.jks
    key-store-password: ${KEYSTORE_PASSWORD}
```

## Documentation Template

Create `SECRETS_INVENTORY.md`:

```markdown
# Secret Inventory

| Name | Type | Rotation | Owner | Status |
|------|------|----------|-------|--------|
| JWT_SECRET | Application | 90 days | DevOps | Active |
| DATABASE_PASSWORD | Database | 60 days | DBA | Active |
| STRIPE_API_KEY | Payment | As-needed | Finance | Active |

## Rotation Schedule
- JWT_SECRET: Every 90 days (next: 2024-04-15)
- DATABASE_PASSWORD: Every 60 days (next: 2024-03-15)
```

## Tools & Utilities

### Local Development
- **Keepass**: Password manager
- **Vault**: Local secret storage
- **LastPass**: Team password sharing

### Production
- **AWS Secrets Manager**: AWS native
- **HashiCorp Vault**: Multi-cloud
- **Azure Key Vault**: Azure native
- **Google Secret Manager**: Google Cloud native

### Monitoring
- **Falco**: Runtime security
- **Wazuh**: SIEM
- **Datadog**: Secrets scanning

---

## Summary

✅ **Generate** secrets using cryptographically secure methods
✅ **Store** secrets in secure location (not Git)
✅ **Rotate** secrets regularly (90 days)
✅ **Audit** all secret access
✅ **Encrypt** secrets at rest and in transit
✅ **Limit** access to authorized users only
✅ **Document** secret inventory and rotation schedule
✅ **Monitor** for unauthorized access
✅ **Respond** quickly to compromises

**Remember:** A single leaked secret can compromise your entire application. Treat secrets with extreme care.
