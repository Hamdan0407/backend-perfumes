#!/bin/bash
# Production Startup Script for Perfume Shop
# Author: System Administrator
# Last Updated: 2026-02-02

set -e  # Exit on any error

echo "🚀 Starting Perfume Shop in PRODUCTION mode..."
echo "================================================"

# Check if .env.production file exists
if [ ! -f ".env.production" ]; then
    echo "❌ ERROR: .env.production file not found!"
    echo "Please create .env.production with all required environment variables."
    echo "Copy .env.production.example and update the values."
    exit 1
fi

# Load production environment variables
set -a  # Automatically export all variables
source .env.production
set +a

# Security validations
echo "🔒 Performing security checks..."

if [[ "$JWT_SECRET" == "CHANGE_ME_"* ]] || [[ ${#JWT_SECRET} -lt 32 ]]; then
    echo "❌ ERROR: JWT_SECRET is not set or is using default placeholder!"
    echo "Please set a secure JWT_SECRET in .env.production"
    exit 1
fi

if [[ "$DATABASE_PASSWORD" == "CHANGE_ME_"* ]] || [[ ${#DATABASE_PASSWORD} -lt 12 ]]; then
    echo "❌ ERROR: DATABASE_PASSWORD is weak or using default!"
    echo "Please set a strong DATABASE_PASSWORD in .env.production"
    exit 1
fi

if [[ "$RAZORPAY_KEY_ID" == *"_test_"* ]]; then
    echo "❌ ERROR: Still using Razorpay TEST keys in production!"
    echo "Please update to LIVE Razorpay keys in .env.production"
    exit 1
fi

echo "✅ Security checks passed"

# Check if JAR file exists
JAR_FILE="target/perfume-shop-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ ERROR: JAR file not found: $JAR_FILE"
    echo "Please run: mvn clean package -DskipTests"
    exit 1
fi

echo "✅ JAR file found: $JAR_FILE"

# Check database connectivity (optional)
echo "🔗 Checking database connectivity..."
if command -v pg_isready &> /dev/null; then
    # Extract host and port from DATABASE_URL
    DB_HOST=$(echo $DATABASE_URL | sed -n 's|.*://[^@]*@\([^:]*\):\([0-9]*\)/.*|\1|p')
    DB_PORT=$(echo $DATABASE_URL | sed -n 's|.*://[^@]*@\([^:]*\):\([0-9]*\)/.*|\2|p')
    
    if [ ! -z "$DB_HOST" ] && [ ! -z "$DB_PORT" ]; then
        if pg_isready -h $DB_HOST -p $DB_PORT -U $DATABASE_USERNAME &> /dev/null; then
            echo "✅ Database is reachable"
        else
            echo "⚠️  WARNING: Cannot connect to database. Continuing anyway..."
        fi
    fi
else
    echo "⚠️  pg_isready not available, skipping database check"
fi

# Check Redis connectivity (optional)
echo "🔗 Checking Redis connectivity..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT ping &> /dev/null; then
        echo "✅ Redis is reachable"
    else
        echo "⚠️  WARNING: Cannot connect to Redis. Continuing anyway..."
    fi
else
    echo "⚠️  redis-cli not available, skipping Redis check"
fi

# Create logs directory if it doesn't exist
mkdir -p logs

echo "📋 Production Configuration:"
echo "  - Profile: production"
echo "  - Port: $PORT"
echo "  - Database: $(echo $DATABASE_URL | sed 's/:[^:]*@/:***@/')"  # Hide password
echo "  - Redis: $REDIS_HOST:$REDIS_PORT"
echo "  - Log Level: $LOG_LEVEL"
echo "  - JWT Expiry: ${JWT_EXPIRATION}ms"

echo ""
echo "🚀 Starting application..."
echo "================================================"

# Start the application with production profile
java \
  $JAVA_OPTS \
  -Dspring.profiles.active=production \
  -Dfile.encoding=UTF-8 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar "$JAR_FILE" \
  --server.port=$PORT

echo ""
echo "🛑 Application has stopped."