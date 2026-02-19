# Monitoring Setup Guide (Free & Open Source)

## Overview
This guide sets up a complete monitoring stack using **Prometheus** (metrics collection) and **Grafana** (visualization).

**Cost:** $0 (Open Source)
**Prerequisites:** Docker & Docker Compose

---

## 🚀 Quick Start

### 1. Start Monitoring Stack
Run this command in your project root:

```bash
docker-compose -f docker-compose.monitoring.yml up -d
```

This starts:
- **Prometheus** at `http://localhost:9090`
- **Grafana** at `http://localhost:3001`

### 2. Login to Grafana
- **URL:** [http://localhost:3001](http://localhost:3001)
- **User:** `admin`
- **Password:** `admin` (you'll be asked to change it)

### 3. Connect Prometheus Data Source
✅ **This is now automatic!** 

We have configured Grafana to automatically connect to Prometheus using the `datasource.yml` provisioning file. You don't need to do anything here.

### 4. Import Dashboard
1. Go to **Dashboards (four squares icon)** -> **Import**
2. Enter Dashboard ID: `4701` (JVM (Micrometer)) or `11378` (Spring Boot 2.1 System Monitor)
3. Click **Load**
4. Select "Prometheus" as the data source
5. Click **Import**

---

## 📊 What You Can Monitor

### JVM Metrics
- **Memory Usage:** Heap vs Non-Heap
- **CPU Usage:** System vs Process
- **Threads:** Live, Daemon, Peak
- **Garbage Collection:** Pauses and frequency

### Application Metrics (`/actuator/metrics`)
- **HTTP Requests:** Rate, duration, errors
- **Database Connections:** Active, idle, pending
- **Cache Hit/Miss Rates:** Redis performance

---

## 🔧 Configuration Details

### Spring Boot Config (`application-production.yml`)
Already configured for you:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Prometheus Config (`monitoring/prometheus.yml`)
Tells Prometheus where to satisfy metrics from:
```yaml
scrape_configs:
  - job_name: 'perfume-shop'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

---

## 🔍 Troubleshooting

**"Target Down" in Prometheus**
- Check if your app is running (`http://localhost:8080/actuator/prometheus`)
- `host.docker.internal` works on Windows/Mac. On Linux, use `--network host` or actual IP.

**Grafana Login Failed**
- Default user/pass is admin/admin. Reset container if locked out:
  ```bash
  docker-compose -f docker-compose.monitoring.yml down -v
  docker-compose -f docker-compose.monitoring.yml up -d
  ```
