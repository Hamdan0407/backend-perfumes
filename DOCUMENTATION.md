# Perfume Shop - Complete Documentation

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Installation & Setup](#installation--setup)
5. [Running the Project](#running-the-project)
6. [API Documentation](#api-documentation)
7. [Database Schema](#database-schema)
8. [Features](#features)
9. [Deployment](#deployment)
10. [Troubleshooting](#troubleshooting)

---

## 🎯 Project Overview

**Perfume Shop** is a full-stack e-commerce application designed for buying and selling perfumes online. It provides a complete shopping experience with product browsing, cart management, checkout, payment integration, and order tracking.

**Project Status:** 95-98% Complete ✅

### Key Highlights
- ✅ Full-stack e-commerce platform
- ✅ JWT-based authentication & authorization
- ✅ Role-based access control (Admin/User)
- ✅ Payment gateway integration (Stripe, Razorpay)
- ✅ Order management system
- ✅ Product review system
- ✅ Cart management
- ✅ Responsive UI
- ✅ Production-ready Docker deployment

---

## 🛠 Tech Stack

### Backend
- **Framework:** Spring Boot 3.2.1
- **Language:** Java 17
- **Security:** Spring Security 6.1 with JWT (JJWT 0.12.3)
- **Database:** MySQL 8.0
- **ORM:** JPA/Hibernate
- **Build Tool:** Maven 3.9
- **Payment:** Stripe (24.16.0), Razorpay (1.3.9)
- **Email:** Spring Mail + Gmail SMTP

### Frontend
- **Framework:** React 18 + Vite
- **Node.js:** 18
- **Styling:** Tailwind CSS 3.3
- **HTTP Client:** Axios
- **Build Tool:** Vite

### Deployment
- **Containerization:** Docker & Docker Compose
- **Database Container:** MySQL 8.0 Alpine
- **Server:** Nginx (for frontend)

---

## 🏗 Architecture

### System Architecture
```
┌─────────────────────────────────────────────────┐
│              Frontend (React + Vite)             │
│            http://localhost:3000                │
│         (Nginx on port 80 in container)         │
└──────────────────────┬──────────────────────────┘
                       │
                       │ HTTP/REST API
                       │
┌──────────────────────▼──────────────────────────┐
│         Backend (Spring Boot 3.2.1)             │
│          http://localhost:8080                 │
│  (API on 8080, Debug on 9090 in container)     │
└──────────────────────┬──────────────────────────┘
                       │
                       │ JDBC
                       │
┌──────────────────────▼──────────────────────────┐
│         Database (MySQL 8.0)                    │
│         localhost:3307 (mapped from 3306)      │
│         Database: perfume_shop                 │
└─────────────────────────────────────────────────┘
```

### Application Layers

**Controller Layer** → **Service Layer** → **Repository Layer** → **Database**

#### Core Packages
```
com.perfume.shop/
├── controller/           # REST API endpoints
│   ├── AuthController
│   ├── ProductController
│   ├── CartController
│   ├── OrderController
│   ├── PaymentController
│   ├── ReviewController
│   └── AdminController
├── service/             # Business logic
│   ├── AuthService
│   ├── ProductService
│   ├── CartService
│   ├── OrderService
│   ├── EmailService
│   └── ReviewService
├── repository/          # Database access
│   ├── UserRepository
│   ├── ProductRepository
│   ├── CartRepository
│   ├── OrderRepository
│   └── ReviewRepository
├── entity/              # JPA entities
│   ├── User
│   ├── Product
│   ├── Cart / CartItem
│   ├── Order / OrderItem
│   └── Review
├── dto/                 # Data transfer objects
├── security/            # Security config
│   ├── JwtService
│   ├── JwtAuthenticationFilter
│   └── SecurityConfig
└── exception/           # Global exception handling
```

---

## 💾 Installation & Setup

### Prerequisites
- Docker & Docker Compose installed
- Git (for cloning)
- Optional: Java 17, Node.js 18, Maven 3.9 (for local development)

### Step 1: Clone the Repository
```bash
cd c:\Users\Hamdaan\Documents\maam
```

### Step 2: Configure Environment (Optional)
Create a `.env` file in the project root for custom configuration:

```env
# Database
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=prod_password
DB_ROOT_PASSWORD=root

# JWT Security (IMPORTANT: Change in production!)
JWT_SECRET=your-256-bit-secret-key-min-32-chars

# Email Configuration (Gmail)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Payment Gateways (Optional)
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

RAZORPAY_KEY_ID=your_key_id
RAZORPAY_KEY_SECRET=your_key_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# CORS
CORS_ORIGINS=http://localhost:3000
FRONTEND_URL=http://localhost:3000
```

### Step 3: Build & Run with Docker Compose
```bash
# Clean up old containers/volumes
docker compose down -v

# Start all services (with build)
docker compose up --build -d

# Check status
docker ps
```

**Expected Output:**
```
CONTAINER ID   IMAGE           STATUS
...            perfume-shop-frontend   Up (healthy)
...            perfume-shop-api        Up (healthy)
...            perfume-shop-db         Up (healthy)
```

### Step 4: Verify Services
```bash
# Test API
curl http://localhost:8080/api/products

# Test Frontend
open http://localhost:3000
```

---

## 🚀 Running the Project

### Via Docker Compose (Recommended)
```bash
# Start all services
docker compose up --build

# Stop all services
docker compose down

# View logs
docker compose logs -f perfume-shop-api
docker compose logs -f perfume-shop-frontend
docker compose logs -f perfume-shop-db

# Access services
Frontend: http://localhost:3000
API: http://localhost:8080
Database: localhost:3307
```

### Via Local Environment (Development)

**Backend:**
```bash
cd src/main/java/com/perfume/shop
mvn clean install
mvn spring-boot:run
# Runs on http://localhost:8080
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

**Database:**
```bash
# Docker only
docker run -d \
  --name mysql-perfume \
  -e MYSQL_DATABASE=perfume_shop \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3306:3306 \
  mysql:8.0
```

---

## 📡 API Documentation

### Base URL
- Local: `http://localhost:8080/api`
- Production: `https://api.perfumeshop.com/api`

### Authentication
All protected endpoints require JWT token in header:
```
Authorization: Bearer <JWT_TOKEN>
```

### Endpoints Summary

#### Authentication (`/api/auth`)
| Method | Endpoint | Auth Required | Description |
|--------|----------|---|---|
| POST | `/register` | No | Register new user |
| POST | `/login` | No | Login user, returns JWT |
| POST | `/refresh` | Yes | Refresh JWT token |
| POST | `/logout` | Yes | Logout user |

#### Products (`/api/products`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|---|
| GET | `/` | No | Get all products (paginated) |
| GET | `/{id}` | No | Get product details |
| POST | `/` | Yes (Admin) | Create new product |
| PUT | `/{id}` | Yes (Admin) | Update product |
| DELETE | `/{id}` | Yes (Admin) | Delete product |

#### Cart (`/api/cart`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|---|
| GET | `/` | Yes | Get user's cart |
| POST | `/add` | Yes | Add item to cart |
| PUT | `/{itemId}` | Yes | Update cart item qty |
| DELETE | `/{itemId}` | Yes | Remove from cart |
| DELETE | `/` | Yes | Clear entire cart |

#### Orders (`/api/orders`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|---|
| GET | `/` | Yes | Get user's orders |
| GET | `/{id}` | Yes | Get order details |
| POST | `/checkout` | Yes | Create new order |
| PUT | `/{id}/status` | Yes (Admin) | Update order status |

#### Payments (`/api/payments`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|---|
| POST | `/stripe/intent` | Yes | Create Stripe payment intent |
| POST | `/razorpay/order` | Yes | Create Razorpay order |
| POST | `/webhook/stripe` | No | Stripe webhook handler |
| POST | `/webhook/razorpay` | No | Razorpay webhook handler |

#### Reviews (`/api/reviews`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|---|
| GET | `/product/{id}` | No | Get product reviews |
| POST | `/` | Yes | Create review |
| PUT | `/{id}` | Yes (Owner) | Update review |
| DELETE | `/{id}` | Yes (Owner) | Delete review |

#### Admin (`/api/admin`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|---|
| GET | `/dashboard` | Yes (Admin) | Admin dashboard stats |
| GET | `/users` | Yes (Admin) | List all users |
| GET | `/orders` | Yes (Admin) | List all orders |
| PUT | `/users/{id}/role` | Yes (Admin) | Change user role |

### Example API Calls

**Register User:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
```
Response includes JWT token.

**Get Products:**
```bash
curl http://localhost:8080/api/products?page=0&size=12
```

**Add to Cart:**
```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

---

## 🗄 Database Schema

### Tables Overview

#### `user` Table
```sql
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('USER', 'ADMIN') DEFAULT 'USER',
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### `product` Table
```sql
CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  stock INT NOT NULL,
  category VARCHAR(100),
  image_url VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### `cart` Table
```sql
CREATE TABLE cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id)
);
```

#### `cart_item` Table
```sql
CREATE TABLE cart_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  FOREIGN KEY (cart_id) REFERENCES cart(id),
  FOREIGN KEY (product_id) REFERENCES product(id)
);
```

#### `order` Table
```sql
CREATE TABLE `order` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10, 2) NOT NULL,
  status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
  payment_method VARCHAR(100),
  shipping_address TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id)
);
```

#### `order_item` Table
```sql
CREATE TABLE order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  FOREIGN KEY (order_id) REFERENCES `order`(id),
  FOREIGN KEY (product_id) REFERENCES product(id)
);
```

#### `review` Table
```sql
CREATE TABLE review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES product(id),
  FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### Relationships
```
user (1) ──── (many) cart
user (1) ──── (many) order
user (1) ──── (many) review
product (1) ──── (many) cart_item
product (1) ──── (many) order_item
product (1) ──── (many) review
cart (1) ──── (many) cart_item
order (1) ──── (many) order_item
```

---

## ✨ Features

### User Features
- ✅ User registration & login with JWT authentication
- ✅ Browse products with search & filter
- ✅ View product details & reviews
- ✅ Add/remove items from cart
- ✅ Checkout with cart summary
- ✅ Multiple payment methods (Stripe, Razorpay)
- ✅ Order tracking & history
- ✅ Write product reviews & ratings
- ✅ User profile management

### Admin Features
- ✅ Admin dashboard with statistics
- ✅ Product management (CRUD)
- ✅ Order management & status updates
- ✅ User management
- ✅ Review moderation
- ✅ Sales analytics

### Technical Features
- ✅ JWT-based authentication
- ✅ Role-based access control
- ✅ Global exception handling
- ✅ Request validation
- ✅ Pagination for list endpoints
- ✅ Payment webhook handling
- ✅ Email notifications
- ✅ Automatic admin user initialization
- ✅ CORS enabled for frontend
- ✅ Security headers configured

---

## 🚢 Deployment

### Production Deployment Steps

#### 1. Prepare Server
```bash
# Install Docker & Docker Compose
sudo apt-get update
sudo apt-get install docker.io docker-compose -y

# Clone repository
git clone <repo-url> /opt/perfume-shop
cd /opt/perfume-shop
```

#### 2. Configure Environment
Create `.env` file with production values:
```env
JWT_SECRET=<generate-32-char-random-string>
DATABASE_PASSWORD=<strong-password>
MAIL_USERNAME=<noreply-email>
MAIL_PASSWORD=<app-password>
STRIPE_API_KEY=<production-key>
RAZORPAY_KEY_ID=<production-id>
FRONTEND_URL=https://perfumeshop.com
```

#### 3. Build & Deploy
```bash
# Build images
docker compose build

# Start services
docker compose up -d

# Verify
docker ps
```

#### 4. Setup Reverse Proxy (Nginx)
```nginx
upstream api {
  server perfume-shop-api:8080;
}

server {
  listen 80;
  server_name api.perfumeshop.com;
  
  location / {
    proxy_pass http://api;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }
}
```

#### 5. SSL Certificate (Let's Encrypt)
```bash
sudo certbot certonly --standalone -d api.perfumeshop.com
# Update nginx config with SSL cert path
```

### Hosting Options
- **AWS EC2** + RDS: $30-50/month
- **DigitalOcean Droplet**: $20-40/month
- **Linode**: $15-35/month
- **Google Cloud**: $25-50/month

---

## 🔍 Troubleshooting

### Common Issues & Solutions

#### 1. Docker Services Not Starting
```bash
# Check logs
docker compose logs -f

# Rebuild images
docker compose down -v
docker compose up --build

# Check port conflicts
netstat -an | grep 3000
netstat -an | grep 8080
```

#### 2. Database Connection Error
```bash
# Verify MySQL is running
docker ps | grep mysql

# Check MySQL logs
docker logs perfume-shop-db

# Test connection
docker exec perfume-shop-db mysql -u prod_user -pprod_password -e "SELECT 1"
```

#### 3. API Returns 401 Unauthorized
- Ensure JWT token is included in `Authorization` header
- Token format: `Bearer <token>`
- Check token expiration in logs

#### 4. Frontend Can't Connect to API
- Verify API is running: `curl http://localhost:8080/api/products`
- Check CORS configuration in `SecurityConfig`
- Ensure frontend `.env` has correct API_URL

#### 5. Payment Gateway Integration Not Working
- Verify API keys in `.env` file
- Check webhook URLs are publicly accessible
- Review payment service logs in `docker logs perfume-shop-api`

#### 6. Email Not Sending
- Verify Gmail app password (not regular password)
- Enable "Less secure app access" or use app password
- Check `MAIL_USERNAME` and `MAIL_PASSWORD` in `.env`

### Performance Optimization

```bash
# Monitor container resources
docker stats

# Check API response time
curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/products

# Enable caching headers (frontend)
# Configure database indexes on frequently queried fields
# Use pagination (default 12 items per page)
```

### Security Checklist
- [ ] Change default admin password
- [ ] Rotate JWT_SECRET in production
- [ ] Enable HTTPS/SSL
- [ ] Use strong database password
- [ ] Configure CORS to specific origins only
- [ ] Enable rate limiting on API
- [ ] Validate all user inputs
- [ ] Keep dependencies updated

---

## 📊 Project Statistics

| Metric | Value |
|--------|-------|
| **Completion** | 95-98% |
| **Backend Files** | 30+ Java classes |
| **Frontend Components** | 10+ React components |
| **Database Tables** | 7 tables |
| **API Endpoints** | 40+ endpoints |
| **Code Lines** | 5000+ |
| **Docker Services** | 3 (API, Frontend, DB) |

---

## 🔐 Security Considerations

### Implemented Security
- ✅ JWT-based authentication
- ✅ Password hashing (BCrypt)
- ✅ Role-based access control
- ✅ CORS protection
- ✅ SQL injection prevention (JPA)
- ✅ XSS protection
- ✅ CSRF token (if applicable)
- ✅ Secure headers
- ✅ Input validation

### Production Recommendations
1. Enable HTTPS/TLS
2. Use environment variables for secrets
3. Implement rate limiting
4. Enable API logging & monitoring
5. Set up database backups
6. Configure firewall rules
7. Implement API authentication key rotation
8. Monitor suspicious activities

---

## 📞 Support & Contact

For issues or questions:
1. Check logs: `docker compose logs -f`
2. Review this documentation
3. Check API status: `http://localhost:8080/api/products`
4. Verify database: `docker ps`

---

## 📄 License

This project is proprietary software. All rights reserved.

---

**Project Status: ✅ Production Ready**

**Last Updated:** January 23, 2026

**Version:** 1.0.0
