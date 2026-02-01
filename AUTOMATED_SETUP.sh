#!/bin/bash
# ============================================================================
# PERFUME SHOP - AUTOMATED DOCKER SETUP SCRIPT (Bash)
# ============================================================================
# This script automates the entire setup process for a fresh machine
# - Generates secure environment variables
# - Creates .env.production file
# - Validates dependencies
# - Builds the backend JAR
# - Provides Docker startup commands
# ============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Functions
success() { echo -e "${GREEN}✅ $*${NC}"; }
error() { echo -e "${RED}❌ $*${NC}"; }
warning() { echo -e "${YELLOW}⚠️  $*${NC}"; }
info() { echo -e "${CYAN}ℹ️  $*${NC}"; }

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     PERFUME SHOP - AUTOMATED DOCKER SETUP                 ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

info "Project directory: $SCRIPT_DIR"
echo ""

# ============================================================================
# STEP 1: VALIDATE PREREQUISITES
# ============================================================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}STEP 1: VALIDATING PREREQUISITES${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

CHECKS_PASSED=0
CHECKS_REQUIRED=0

# Check Docker
info "Checking Docker..."
CHECKS_REQUIRED=$((CHECKS_REQUIRED + 1))
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    success "Docker installed: $DOCKER_VERSION"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    error "Docker not found"
fi

# Check Docker Compose
info "Checking Docker Compose..."
CHECKS_REQUIRED=$((CHECKS_REQUIRED + 1))
if docker compose version &> /dev/null; then
    success "Docker Compose installed"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    error "Docker Compose not found"
fi

# Check Java
info "Checking Java..."
CHECKS_REQUIRED=$((CHECKS_REQUIRED + 1))
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    success "Java installed: $JAVA_VERSION"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    error "Java not found"
fi

# Check Maven
info "Checking Maven..."
CHECKS_REQUIRED=$((CHECKS_REQUIRED + 1))
if command -v mvn &> /dev/null; then
    success "Maven installed"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    error "Maven not found"
fi

# Check disk space
info "Checking disk space..."
CHECKS_REQUIRED=$((CHECKS_REQUIRED + 1))
AVAILABLE=$(df . | tail -1 | awk '{print $4}')
if [ "$AVAILABLE" -gt 5242880 ]; then
    success "Sufficient disk space available"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    error "Insufficient disk space (need 5GB+)"
fi

echo ""
echo -e "${CYAN}Prerequisites: $CHECKS_PASSED/$CHECKS_REQUIRED passed${NC}"
echo ""

if [ "$CHECKS_PASSED" -lt "$CHECKS_REQUIRED" ]; then
    error "Some prerequisites failed. Please install missing tools."
    exit 1
fi

# ============================================================================
# STEP 2: CHECK IF .env.production EXISTS
# ============================================================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}STEP 2: ENVIRONMENT FILE${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

ENV_FILE=".env.production"

if [ -f "$ENV_FILE" ]; then
    info "Found existing $ENV_FILE"
    echo ""
    echo "Do you want to:"
    echo "  1. Keep existing .env.production file"
    echo "  2. Regenerate with new values"
    echo ""
    read -p "Enter choice (1 or 2): " choice
    
    if [ "$choice" = "2" ]; then
        rm "$ENV_FILE"
        success "Removed existing $ENV_FILE"
    elif [ "$choice" = "1" ]; then
        success "Using existing $ENV_FILE"
        warning "Make sure all critical variables are set!"
    else
        error "Invalid choice. Exiting."
        exit 1
    fi
fi

# Generate .env.production if it doesn't exist
if [ ! -f "$ENV_FILE" ]; then
    info "Generating new $ENV_FILE with secure values..."
    echo ""
    
    # Generate secure random values
    JWT_SECRET=$(openssl rand -base64 32)
    DB_PASSWORD=$(openssl rand -base64 16 | tr -d '=' | cut -c1-16)
    
    info "Generated JWT_SECRET"
    info "Generated DATABASE_PASSWORD"
    
    if [ ! -f ".env.production.example" ]; then
        error ".env.production.example not found!"
        exit 1
    fi
    
    # Create .env.production
    cp ".env.production.example" "$ENV_FILE"
    
    # Update values
    sed -i "s/^JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" "$ENV_FILE"
    sed -i "s/^DATABASE_PASSWORD=.*/DATABASE_PASSWORD=$DB_PASSWORD/" "$ENV_FILE"
    sed -i "s/^DATABASE_USERNAME=.*/DATABASE_USERNAME=prod_user/" "$ENV_FILE"
    sed -i "s/^MAIL_USERNAME=.*/MAIL_USERNAME=noreply@perfume.local/" "$ENV_FILE"
    sed -i "s/^MAIL_PASSWORD=.*/MAIL_PASSWORD=test-password-123/" "$ENV_FILE"
    sed -i "s/^RAZORPAY_KEY_ID=.*/RAZORPAY_KEY_ID=rzp_test_00000000000000/" "$ENV_FILE"
    sed -i "s/^RAZORPAY_KEY_SECRET=.*/RAZORPAY_KEY_SECRET=test_secret_key_00000000/" "$ENV_FILE"
    sed -i "s/^RAZORPAY_WEBHOOK_SECRET=.*/RAZORPAY_WEBHOOK_SECRET=webhook_test_secret_key/" "$ENV_FILE"
    sed -i "s/^CORS_ORIGINS=.*/CORS_ORIGINS=http:\/\/localhost:3000,http:\/\/localhost:80/" "$ENV_FILE"
    sed -i "s/^FRONTEND_URL=.*/FRONTEND_URL=http:\/\/localhost:3000/" "$ENV_FILE"
    sed -i "s/^MAIL_HOST=.*/MAIL_HOST=mailhog/" "$ENV_FILE"
    sed -i "s/^MAIL_PORT=.*/MAIL_PORT=1025/" "$ENV_FILE"
    
    success "Created $ENV_FILE with secure values"
    echo ""
    
    warning "⚠️  IMPORTANT NOTES:"
    echo ""
    echo "The .env.production file has been created with:"
    echo "  ✅ Generated JWT_SECRET (cryptographically secure)"
    echo "  ✅ Generated DATABASE_PASSWORD (secure random)"
    echo "  ℹ️  Test values for email, Razorpay (for development/testing)"
    echo ""
else
    success "Using existing $ENV_FILE"
fi

echo ""

# ============================================================================
# STEP 3: BUILD BACKEND JAR
# ============================================================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}STEP 3: BUILDING BACKEND${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ -f "target/perfume-shop-1.0.0.jar" ]; then
    info "Found existing target/perfume-shop-1.0.0.jar"
    read -p "Rebuild JAR? (y/n, default: n): " rebuild
    if [ "$rebuild" = "y" ]; then
        info "Cleaning previous build..."
        mvn clean > /dev/null
        info "Building JAR with Maven..."
        mvn package -DskipTests -q
        if [ $? -eq 0 ]; then
            success "JAR built successfully!"
        else
            error "Maven build failed!"
            exit 1
        fi
    else
        success "Using existing JAR file"
    fi
else
    info "Building JAR with Maven (this may take 2-5 minutes)..."
    mvn package -DskipTests -q
    
    if [ $? -eq 0 ] && [ -f "target/perfume-shop-1.0.0.jar" ]; then
        success "JAR built successfully!"
    else
        error "Maven build failed!"
        exit 1
    fi
fi

echo ""

# ============================================================================
# STEP 4: DOCKER COMPOSE VALIDATION
# ============================================================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}STEP 4: VALIDATING DOCKER SETUP${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

info "Validating docker-compose.yml..."
if docker compose config > /dev/null 2>&1; then
    success "docker-compose.yml is valid"
else
    error "docker-compose.yml has syntax errors!"
    exit 1
fi

echo ""

# ============================================================================
# STEP 5: READY TO START
# ============================================================================
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${CYAN}STEP 5: READY TO START${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

success "All prerequisites validated!"
success "Environment file created/updated: $ENV_FILE"
success "Backend JAR ready: target/perfume-shop-1.0.0.jar"
success "Docker configuration validated"

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  SETUP COMPLETE! YOU'RE READY TO START THE CONTAINERS     ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"

echo ""
echo -e "${CYAN}To start the application, run:${NC}"
echo ""
echo -e "${YELLOW}  docker compose --env-file .env.production up --build${NC}"
echo ""

echo -e "${CYAN}This will:${NC}"
echo "  1. Pull Docker images (MySQL, etc.)"
echo "  2. Build the API container"
echo "  3. Start MySQL database"
echo "  4. Start Spring Boot API (port 8080)"
echo "  5. Show logs in real-time"
echo ""

echo -e "${CYAN}In another terminal, you can verify with:${NC}"
echo ""
echo "  # Check if services are running"
echo -e "${YELLOW}  docker compose ps${NC}"
echo ""
echo "  # Check API health"
echo -e "${YELLOW}  curl http://localhost:8080/actuator/health${NC}"
echo ""

read -p "Start containers now? (y/n): " start_now
if [ "$start_now" = "y" ]; then
    info "Starting Docker containers..."
    echo ""
    docker compose --env-file .env.production up --build
else
    info "Setup complete! Start containers whenever you're ready with:"
    echo -e "${YELLOW}  docker compose --env-file .env.production up --build${NC}"
fi
