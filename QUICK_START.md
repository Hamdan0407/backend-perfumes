# Quick Start Production Deployment

Fast-track guide to deploy Parfumé to production.

## Prerequisites

- Docker and Docker Compose installed
- MySQL 8.0+ (or use Docker MySQL)
- Nginx (for reverse proxy)
- Domain with SSL certificate (Let's Encrypt recommended)
- Payment gateway accounts (Stripe and/or Razorpay)

## Quick Deployment (5 minutes)

### 1. Prepare Environment Files

```bash
# Backend environment
cp .env.production.example .env.production
# Edit .env.production with actual values
nano .env.production

# Frontend environment
cp frontend/.env.production.example frontend/.env.production.local
# Edit with actual API URL
nano frontend/.env.production.local
```

### 2. Build Docker Images

```bash
# Build backend
docker build -t perfume-shop-api:latest .

# Build frontend
docker build -t perfume-shop-frontend:latest ./frontend
```

### 3. Start with Docker Compose

```bash
# Start all services
docker-compose up -d

# Check services
docker-compose ps

# View logs
docker-compose logs -f api
```

### 4. Test Deployment

```bash
# Health check
curl http://localhost:8080/actuator/health

# API test
curl http://localhost:8080/api/products

# Frontend
curl http://localhost
```

### 5. Configure Nginx

```bash
# Copy nginx config
sudo cp nginx.conf /etc/nginx/sites-available/perfume-shop

# Create SSL certificate (Let's Encrypt)
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# Enable site
sudo ln -s /etc/nginx/sites-available/perfume-shop /etc/nginx/sites-enabled/

# Test config
sudo nginx -t

# Reload nginx
sudo systemctl reload nginx
```

## Production Checklist

### Before Going Live

- [ ] **Environment Variables Set**
  ```bash
  cat .env.production | grep -E "JWT_SECRET|DATABASE_PASSWORD|STRIPE|RAZORPAY"
  ```

- [ ] **Database Backup Configured**
  ```bash
  # Create backup
  mysqldump -u prod_user -p perfume_shop | gzip > backup.sql.gz
  ```

- [ ] **SSL Certificate Installed**
  ```bash
  sudo certbot certificates
  ```

- [ ] **Logs Directory Created**
  ```bash
  sudo mkdir -p /var/log/perfume-shop
  sudo chown perfume:perfume /var/log/perfume-shop
  ```

- [ ] **Health Checks Passing**
  ```bash
  curl https://yourdomain.com/actuator/health
  curl https://yourdomain.com/api/products
  ```

- [ ] **Payment Gateways Configured**
  - Stripe test → Live keys
  - Razorpay test → Live keys

- [ ] **Email Service Working**
  ```bash
  # Check email in logs
  grep "Mail" /var/log/perfume-shop/perfume-shop.log
  ```

- [ ] **Monitoring Enabled**
  ```bash
  # Test metrics endpoint
  curl https://yourdomain.com/actuator/metrics
  ```

## Common Commands

### Docker Management

```bash
# View running containers
docker ps

# Check container logs
docker logs perfume-shop-api

# Stop services
docker-compose down

# Restart services
docker-compose restart

# Update services
docker-compose pull
docker-compose up -d

# Remove all data
docker-compose down -v
```

### Database Management

```bash
# Access database
mysql -u prod_user -p perfume_shop

# Create backup
mysqldump -u prod_user -p perfume_shop | gzip > backup-$(date +%Y%m%d).sql.gz

# Restore from backup
gunzip < backup-20240115.sql.gz | mysql -u prod_user -p perfume_shop
```

### Log Viewing

```bash
# Application logs
tail -100f /var/log/perfume-shop/perfume-shop.log

# Error logs
grep ERROR /var/log/perfume-shop/perfume-shop.log | tail -20

# Security events
tail -f /var/log/perfume-shop/security.log
```

### Performance Monitoring

```bash
# CPU and memory
docker stats perfume-shop-api

# Database connections
curl http://localhost:9090/actuator/metrics/db.connection.active

# Response times
curl http://localhost:9090/actuator/metrics/http.server.requests
```

## Troubleshooting

### Issue: Container Won't Start

**Solution:**
```bash
# Check logs
docker logs perfume-shop-api

# Verify environment
docker inspect perfume-shop-api | grep -A 50 Env

# Restart with debug
docker-compose logs -f
```

### Issue: Database Connection Error

**Solution:**
```bash
# Check MySQL is running
docker-compose ps database

# Test connection
docker exec perfume-shop-db mysql -u prod_user -p -e "SELECT 1"

# Check URL format
echo $DATABASE_URL
```

### Issue: 502 Bad Gateway from Nginx

**Solution:**
```bash
# Check backend is running
curl http://localhost:8080/actuator/health

# Check Nginx config
sudo nginx -t

# Check Nginx logs
sudo tail /var/log/nginx/perfume-shop-error.log
```

### Issue: Slow Requests

**Solution:**
```bash
# Check database connections
curl http://localhost:9090/actuator/metrics/db.connection.active

# Check memory
docker stats perfume-shop-api

# Check request metrics
curl http://localhost:9090/actuator/metrics/http.server.requests
```

## Scaling for High Traffic

### Horizontal Scaling (Multiple Instances)

```yaml
# docker-compose.yml - Add multiple API instances
services:
  api-1:
    # ... same as api
  api-2:
    # ... same as api
  api-3:
    # ... same as api
```

### Vertical Scaling (Larger Instance)

```bash
# Increase JVM memory
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC"
docker-compose up -d
```

### Database Optimization

```bash
# Add database indexes
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_order_user ON orders(user_id);
CREATE INDEX idx_cart_user ON cart(user_id);

# Optimize tables
OPTIMIZE TABLE products, orders, users;
```

## Monitoring Setup

### Using Uptime Robot

1. Create account at uptime.com
2. Add monitor: https://yourdomain.com/actuator/health
3. Set interval: 5 minutes
4. Enable alerts to email

### Using New Relic (Optional)

```bash
# Install New Relic agent
docker exec perfume-shop-api bash -c "curl -O https://download.newrelic.com/..."

# Configure application.yml
app:
  monitoring:
    enabled: true
    service-name: perfume-shop
```

## Backup & Recovery

### Automated Daily Backups

```bash
# Create backup script
cat > /opt/perfume-shop/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/backups/mysql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mysqldump -u prod_user -p$DB_PASSWORD perfume_shop | gzip > $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql.gz
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
EOF

# Add to crontab
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/perfume-shop/backup.sh") | crontab -
```

### Restore from Backup

```bash
# Decompress and restore
gunzip < /backups/mysql/perfume_shop_20240115_020000.sql.gz | mysql -u prod_user -p perfume_shop

# Verify
mysql -u prod_user -p perfume_shop -e "SELECT COUNT(*) FROM users;"
```

## Security Hardening

### Firewall Rules

```bash
# Allow SSH
sudo ufw allow 22/tcp

# Allow HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Allow API only from Nginx
sudo ufw allow from 127.0.0.1 to any port 8080

# Enable firewall
sudo ufw enable
```

### Fail2Ban for Rate Limiting

```bash
# Install
sudo apt install fail2ban

# Create filter
sudo cat > /etc/fail2ban/filter.d/perfume-shop.conf << 'EOF'
[Definition]
failregex = ^<HOST>.* "POST /api/auth/login.*HTTP" 401
ignoreregex =
EOF

# Create jail
sudo systemctl restart fail2ban
```

## Maintenance Schedule

### Daily
- Check error logs for critical issues
- Monitor disk space and backups

### Weekly
- Review security logs
- Check database size and optimize if needed
- Verify backups are working

### Monthly
- Rotate SSL certificates
- Update dependencies
- Review access patterns and optimize queries

### Quarterly
- Security audit
- Load testing
- Disaster recovery drill

## Support & Monitoring

### Alert Examples

Set up alerts for:
- Container restart (failure)
- HTTP 5xx errors > 1%
- Response time > 1 second
- Database connections > 15/20
- Disk usage > 80%
- Failed backups

### Contact Points

- Email alerts to: devops@yourdomain.com
- Slack notifications: #perfume-shop-alerts
- PagerDuty for critical issues

---

## Next Steps

After deployment:

1. ✅ Test all major flows (login, browse, checkout)
2. ✅ Load test with expected traffic
3. ✅ Verify payment processing works
4. ✅ Check email notifications
5. ✅ Test disaster recovery
6. ✅ Document runbook procedures
7. ✅ Train support team
8. ✅ Plan monitoring and alerting

**Estimated Time to Production**: 30 minutes to 2 hours depending on infrastructure setup.
