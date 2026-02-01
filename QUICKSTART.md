# Quick Start Guide

## 🎯 First Time Setup (5 Minutes)

### Step 1: Install Prerequisites
- ✅ Java 17 or higher → [Download](https://adoptium.net/)
- ✅ MySQL 8.0+ → [Download](https://dev.mysql.com/downloads/installer/)
- ✅ Node.js 18+ → [Download](https://nodejs.org/)

### Step 2: Setup Database
```sql
-- Open MySQL Command Line or Workbench
CREATE DATABASE perfume_shop;
```

### Step 3: Configure Backend
Open `src/main/resources/application.yml` and update:
```yaml
datasource:
  username: root          # Your MySQL username
  password: your_password # Your MySQL password

mail:
  username: your-email@gmail.com
  password: xxxx xxxx xxxx xxxx  # Gmail App Password

jwt:
  secret: ChangeThisToALongRandomString256BitsMinimum

stripe:
  api-key: sk_test_your_key_here  # From stripe.com
```

### Step 4: Start Backend
```powershell
# From project root
mvn spring-boot:run
```

Wait for: `Started PerfumeShopApplication` message

### Step 5: Setup Frontend
```powershell
cd frontend
npm install
```

Edit `src/pages/Checkout.jsx` line 12:
```javascript
const stripePromise = loadStripe('pk_test_your_publishable_key');
```

### Step 6: Start Frontend
```powershell
npm run dev
```

### Step 7: Access Application
- 🌐 **Frontend**: http://localhost:3000
- 🔧 **Backend API**: http://localhost:8080
- 📊 **API Docs**: http://localhost:8080/api/products

## 🧪 Testing the Application

### 1. Create Admin Account
```sql
-- Run in MySQL
INSERT INTO users (email, password, first_name, last_name, role, active, created_at, updated_at) 
VALUES ('admin@test.com', '$2a$10$rCKnEpnNdnzMm4jJlLGOyu0HVJVvCrMm1c7Uvc0L.MH/WjLGf8sFW', 'Admin', 'Test', 'ADMIN', true, NOW(), NOW());
-- Password is: admin123
```

### 2. Add Sample Products
```sql
INSERT INTO products (name, brand, description, price, stock, category, type, volume, image_url, featured, active, rating, review_count, created_at, updated_at) VALUES
('Chanel No. 5', 'Chanel', 'Iconic floral fragrance', 150.00, 50, 'Women', 'Eau de Parfum', 100, 'https://images.unsplash.com/photo-1541643600914-78b084683601?w=400', true, true, 4.8, 120, NOW(), NOW()),
('Dior Sauvage', 'Dior', 'Fresh spicy scent', 120.00, 75, 'Men', 'Eau de Toilette', 100, 'https://images.unsplash.com/photo-1595425970377-c9703cf48b6d?w=400', true, true, 4.7, 98, NOW(), NOW());
```

### 3. Test User Flow
1. Go to http://localhost:3000
2. Click "Sign Up" → Create account
3. Browse products
4. Add to cart
5. Go to checkout
6. Use test card: `4242 4242 4242 4242` (any future date, any CVC)

### 4. Test Admin Flow
1. Login as admin@test.com / admin123
2. Click "Admin" in navbar
3. Use API endpoints to manage products

## 🔍 API Testing with Postman

### Import Collection
```json
{
  "name": "Perfume Shop API",
  "item": [
    {
      "name": "Register",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/auth/register",
        "body": {
          "email": "user@test.com",
          "password": "password123",
          "firstName": "John",
          "lastName": "Doe"
        }
      }
    },
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/auth/login",
        "body": {
          "email": "user@test.com",
          "password": "password123"
        }
      }
    },
    {
      "name": "Get Products",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/products"
      }
    }
  ]
}
```

## 📧 Gmail Configuration
1. Go to https://myaccount.google.com/
2. Security → 2-Step Verification (Enable)
3. Security → App passwords
4. Select app: Mail, Device: Other → Generate
5. Copy 16-character password to `application.yml`

## 💳 Stripe Configuration
1. Sign up at https://stripe.com
2. Dashboard → Developers → API keys
3. Copy "Publishable key" and "Secret key"
4. For webhooks:
   - Install Stripe CLI: https://stripe.com/docs/stripe-cli
   - Run: `stripe listen --forward-to localhost:8080/api/payment/webhook`
   - Copy webhook signing secret

## 🐛 Common Errors & Fixes

| Error | Solution |
|-------|----------|
| Port 8080 already in use | Stop other Java apps or change port in application.yml |
| Cannot connect to MySQL | Start MySQL service: `net start MySQL80` |
| JWT token errors | Clear browser localStorage and login again |
| Stripe payment fails | Use test card 4242... and ensure API keys are correct |
| Email not sending | Verify app password and 2FA enabled |

## 📱 Mobile Testing
The frontend is responsive! Test on:
- Chrome DevTools (F12 → Device Toolbar)
- Your phone (use `http://YOUR_IP:3000`)

## 🎓 Learning Path

### Week 1: Understand Structure
- [ ] Explore entity classes (User, Product, Order)
- [ ] Understand JPA relationships
- [ ] Review repository queries

### Week 2: API Development
- [ ] Study controller methods
- [ ] Learn request/response DTOs
- [ ] Test with Postman

### Week 3: Security
- [ ] JWT authentication flow
- [ ] Spring Security configuration
- [ ] Role-based access

### Week 4: Integration
- [ ] Stripe payment flow
- [ ] Email service
- [ ] Frontend-backend communication

## 💡 Next Steps
1. Add product image upload functionality
2. Implement wishlist feature
3. Add admin dashboard UI
4. Create mobile app with React Native
5. Deploy to production (Heroku + Vercel)

---

Need help? Check the main README.md for detailed documentation!
