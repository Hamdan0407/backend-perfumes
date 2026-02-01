# 🌸 Parfumé - Luxury Perfume E-Commerce Platform

A **production-ready** full-stack e-commerce application built with **Spring Boot** and **React**. This project is designed to help you learn Spring Boot development through a real-world, feature-rich application.

## ✨ Features

### 🛍️ **Customer Features**
- **Product Catalog**: Browse perfumes by category (Men, Women, Unisex), brand, price range
- **Advanced Search**: Search products with filters and sorting options
- **Product Details**: Detailed product pages with images, reviews, ratings, and fragrance notes
- **Shopping Cart**: Add/remove items, update quantities with real-time price calculations
- **Secure Checkout**: Multi-step checkout with Stripe payment integration
- **Order Management**: View order history, track orders, and receive email notifications
- **User Reviews**: Rate and review products
- **Authentication**: Secure JWT-based registration and login

### 👨‍💼 **Admin Features**
- **Product Management**: Create, update, delete products
- **Order Management**: View all orders, update order status, add tracking numbers
- **Inventory Control**: Manage stock levels
- **Featured Products**: Mark products as featured for homepage display

### 🔒 **Security Features**
- JWT (JSON Web Token) authentication
- Role-based access control (USER, ADMIN)
- Password encryption with BCrypt
- CORS configuration for frontend integration
- Secure payment processing with Stripe

### 📧 **Email Notifications**
- Order confirmation emails
- Order status updates
- Shipping notifications with tracking numbers
- HTML email templates

## 🏗️ Technology Stack

### Backend
- **Spring Boot 3.2.1** - Main framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database operations
- **MySQL** - Production database
- **H2** - Development/testing database
- **JWT (jjwt 0.12.3)** - Token-based authentication
- **Stripe API** - Payment processing
- **JavaMail** - Email notifications
- **Lombok** - Reduce boilerplate code
- **Maven** - Dependency management

### Frontend
- **React 18** - UI framework
- **Vite** - Build tool and dev server
- **React Router 6** - Client-side routing
- **Zustand** - State management
- **Axios** - HTTP client
- **Tailwind CSS** - Styling
- **Stripe Elements** - Payment UI components
- **React Toastify** - Notifications

## 📁 Project Structure

```
perfume-shop/
├── src/main/java/com/perfume/shop/
│   ├── controller/          # REST API endpoints
│   │   ├── AuthController.java
│   │   ├── ProductController.java
│   │   ├── CartController.java
│   │   ├── OrderController.java
│   │   ├── ReviewController.java
│   │   ├── PaymentController.java
│   │   └── AdminController.java
│   ├── service/             # Business logic
│   │   ├── AuthService.java
│   │   ├── ProductService.java
│   │   ├── CartService.java
│   │   ├── OrderService.java
│   │   ├── ReviewService.java
│   │   └── EmailService.java
│   ├── repository/          # Database access
│   │   ├── UserRepository.java
│   │   ├── ProductRepository.java
│   │   ├── CartRepository.java
│   │   ├── OrderRepository.java
│   │   └── ReviewRepository.java
│   ├── entity/              # JPA entities
│   │   ├── User.java
│   │   ├── Product.java
│   │   ├── Cart.java
│   │   ├── Order.java
│   │   └── Review.java
│   ├── dto/                 # Data Transfer Objects
│   ├── security/            # Security configuration
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   ├── exception/           # Exception handling
│   └── PerfumeShopApplication.java
├── src/main/resources/
│   ├── application.yml      # Development configuration
│   └── application-prod.yml # Production configuration
└── pom.xml                  # Maven dependencies

frontend/
├── src/
│   ├── components/          # Reusable components
│   │   ├── Navbar.jsx
│   │   ├── Footer.jsx
│   │   ├── ProductCard.jsx
│   │   ├── PrivateRoute.jsx
│   │   └── AdminRoute.jsx
│   ├── pages/               # Page components
│   │   ├── Home.jsx
│   │   ├── Products.jsx
│   │   ├── ProductDetail.jsx
│   │   ├── Cart.jsx
│   │   ├── Checkout.jsx
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Orders.jsx
│   │   └── OrderDetail.jsx
│   ├── store/               # State management
│   │   ├── authStore.js
│   │   └── cartStore.js
│   ├── api/                 # API configuration
│   │   └── axios.js
│   ├── App.jsx              # Main app component
│   └── main.jsx             # Entry point
├── package.json
└── vite.config.js
```

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+**
- **Node.js 18+** and npm
- **MySQL 8.0+** (or use H2 for development)
- **Stripe Account** (for payment testing)
- **Gmail Account** (for email notifications)

### 1. Clone the Repository
```bash
cd "C:\Users\Hamdaan\Documents\maam"
```

### 2. Backend Setup

#### Configure Database
Create a MySQL database:
```sql
CREATE DATABASE perfume_shop;
```

#### Configure Application Properties
Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/perfume_shop
    username: your_mysql_username
    password: your_mysql_password
  
  mail:
    username: your-email@gmail.com
    password: your-app-password  # Generate from Google Account settings

app:
  jwt:
    secret: YourSecureJWTSecretKeyHere  # Change this!
  
  stripe:
    api-key: sk_test_your_stripe_secret_key
    webhook-secret: whsec_your_webhook_secret
```

#### Run the Backend
```bash
# Using Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/perfume-shop-1.0.0.jar
```

Backend will start on `http://localhost:8080`

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Update Stripe Publishable Key
# Edit src/pages/Checkout.jsx and replace:
# const stripePromise = loadStripe('pk_test_your_stripe_publishable_key');

# Start development server
npm run dev
```

Frontend will start on `http://localhost:3000`

## 🔑 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login

### Products
- `GET /api/products` - Get all products (paginated)
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/category/{category}` - Filter by category
- `GET /api/products/brand/{brand}` - Filter by brand
- `GET /api/products/search?query={query}` - Search products
- `GET /api/products/featured` - Get featured products
- `GET /api/products/brands` - Get all brands

### Cart (Requires Authentication)
- `GET /api/cart` - Get user's cart
- `POST /api/cart/items` - Add item to cart
- `PUT /api/cart/items/{id}` - Update cart item quantity
- `DELETE /api/cart/items/{id}` - Remove item from cart

### Orders (Requires Authentication)
- `POST /api/orders/checkout` - Create order and payment intent
- `GET /api/orders` - Get user's orders
- `GET /api/orders/{id}` - Get order details

### Reviews (Requires Authentication)
- `POST /api/reviews` - Create review
- `GET /api/reviews/product/{productId}` - Get product reviews
- `PUT /api/reviews/{id}` - Update review
- `DELETE /api/reviews/{id}` - Delete review

### Admin (Requires ADMIN role)
- `POST /api/admin/products` - Create product
- `PUT /api/admin/products/{id}` - Update product
- `DELETE /api/admin/products/{id}` - Delete product
- `GET /api/admin/orders` - Get all orders
- `PUT /api/admin/orders/{id}/status` - Update order status
- `PUT /api/admin/orders/{id}/tracking` - Add tracking number

### Payment
- `POST /api/payment/webhook` - Stripe webhook endpoint

## 📝 Sample Data

Create sample products using the Admin API or directly insert into database:

```sql
INSERT INTO products (name, brand, description, price, stock, category, type, volume, image_url, featured, active, rating, review_count, created_at, updated_at) 
VALUES 
('Chanel No. 5', 'Chanel', 'The iconic fragrance with notes of jasmine and rose', 150.00, 50, 'Women', 'Eau de Parfum', 100, 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=400', true, true, 4.8, 120, NOW(), NOW()),
('Dior Sauvage', 'Dior', 'Fresh and spicy fragrance for men', 120.00, 75, 'Men', 'Eau de Toilette', 100, 'https://images.unsplash.com/photo-1595425970377-c9703cf48b6d?w=400', true, true, 4.7, 98, NOW(), NOW()),
('Tom Ford Black Orchid', 'Tom Ford', 'Luxurious unisex oriental fragrance', 180.00, 30, 'Unisex', 'Eau de Parfum', 50, 'https://images.unsplash.com/photo-1594035910387-fea47794261f?w=400', true, true, 4.9, 156, NOW(), NOW());
```

Create an admin user:
```sql
-- Password: admin123 (BCrypt hashed)
INSERT INTO users (email, password, first_name, last_name, role, active, created_at, updated_at) 
VALUES ('admin@perfume.com', '$2a$10$rCKnEpnNdnzMm4jJlLGOyu0HVJVvCrMm1c7Uvc0L.MH/WjLGf8sFW', 'Admin', 'User', 'ADMIN', true, NOW(), NOW());
```

## 🔧 Configuration Guide

### Email Setup (Gmail)
1. Go to [Google Account Settings](https://myaccount.google.com/)
2. Enable 2-Factor Authentication
3. Generate App Password: Security → 2-Step Verification → App passwords
4. Use the generated password in `application.yml`

### Stripe Setup
1. Create account at [Stripe](https://stripe.com)
2. Get test API keys from Dashboard → Developers → API keys
3. Set up webhook:
   - URL: `http://localhost:8080/api/payment/webhook`
   - Events: `payment_intent.succeeded`, `payment_intent.payment_failed`
4. Copy webhook secret

### Database Options

**Development (H2):**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

**Production (MySQL):**
```yaml
spring:
  datasource:
    url: jdbc:mysql://your-server:3306/perfume_shop
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## 📚 Learning Resources

### Key Spring Boot Concepts Covered
1. **RESTful API Development** - Building well-structured REST endpoints
2. **JPA & Hibernate** - Database operations and relationships
3. **Spring Security** - Authentication, authorization, JWT
4. **Exception Handling** - Global exception handler
5. **Validation** - Input validation with Bean Validation
6. **Service Layer Pattern** - Separation of concerns
7. **DTO Pattern** - Data transfer objects
8. **Transaction Management** - @Transactional usage
9. **Third-party Integration** - Stripe, JavaMail
10. **Configuration Management** - application.yml, profiles

### Project Highlights for Learning
- **Entity Relationships**: OneToMany, ManyToOne, OneToOne
- **Custom Queries**: JPQL queries in repositories
- **Pagination & Sorting**: Spring Data pagination
- **File Upload** (can be added): MultipartFile handling
- **Async Processing**: @Async for email sending
- **Security Filters**: Custom JWT authentication filter
- **CORS Configuration**: Frontend-backend integration

## 🚀 Deployment

### Backend (Spring Boot)
```bash
# Build for production
mvn clean package -DskipTests

# Run with production profile
java -jar target/perfume-shop-1.0.0.jar --spring.profiles.active=prod
```

### Frontend (React)
```bash
cd frontend
npm run build
# Deploy 'dist' folder to your hosting service
```

### Recommended Hosting
- **Backend**: Heroku, AWS Elastic Beanstalk, Railway, Render
- **Frontend**: Vercel, Netlify, AWS S3 + CloudFront
- **Database**: AWS RDS, PlanetScale, Railway

## 🐛 Troubleshooting

### Common Issues

**1. Database Connection Error**
- Verify MySQL is running
- Check username/password in application.yml
- Ensure database exists

**2. JWT Token Errors**
- Check JWT secret is properly configured
- Ensure token is sent in Authorization header as "Bearer {token}"

**3. Stripe Payment Fails**
- Use test card: 4242 4242 4242 4242
- Verify API keys are correct
- Check webhook is configured

**4. Email Not Sending**
- Verify Gmail app password is correct
- Check 2FA is enabled on Google account
- Review firewall/antivirus settings

**5. CORS Errors**
- Verify frontend URL in SecurityConfig
- Check proxy configuration in vite.config.js

## 📄 License

This project is created for educational purposes.

## 🤝 Contributing

This is a learning project. Feel free to fork and modify for your own learning!

## 📞 Support

For questions about Spring Boot concepts or implementation:
- Review the code comments
- Check Spring Boot documentation
- Experiment with the API using Postman/Insomnia

---

**Happy Learning! 🎓**

Built with ❤️ to help you master Spring Boot development.
