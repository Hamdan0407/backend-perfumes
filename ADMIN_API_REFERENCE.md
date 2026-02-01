# Admin Panel - API Reference & Usage Guide

## Complete API Reference for Admin Panel

---

## Authentication Header Format

All API requests require the following header:
```
Authorization: Bearer {accessToken}
```

Replace `{accessToken}` with the token obtained after login.

---

## 1. Products API

### Get All Products
```http
GET http://localhost:8080/api/products?size=100&page=0
Authorization: Bearer {token}
```

**Response Example:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Chanel No. 5",
      "category": "Women",
      "price": 150.00,
      "stock": 50,
      "rating": 4.8,
      "description": "Classic perfume..."
    }
  ],
  "pageable": {...},
  "totalElements": 50,
  "totalPages": 5
}
```

### Create Product
```http
POST http://localhost:8080/api/products
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "New Perfume",
  "category": "Women",
  "price": 120.00,
  "stock": 30,
  "description": "Description here"
}
```

### Update Product
```http
PUT http://localhost:8080/api/products/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Updated Name",
  "price": 130.00,
  "stock": 25
}
```

### Delete Product
```http
DELETE http://localhost:8080/api/products/{id}
Authorization: Bearer {token}
```

**Response:** 204 No Content on success

---

## 2. Orders API

### Get All Orders
```http
GET http://localhost:8080/api/orders?size=50&page=0
Authorization: Bearer {token}
```

**Response Example:**
```json
{
  "content": [
    {
      "id": 1,
      "customerName": "John Doe",
      "totalAmount": 299.99,
      "status": "COMPLETED",
      "createdAt": "2024-01-27T10:00:00Z",
      "user": {
        "id": 1,
        "firstName": "John",
        "lastName": "Doe",
        "email": "john@example.com"
      }
    }
  ],
  "pageable": {...},
  "totalElements": 120,
  "totalPages": 3
}
```

### Get Order Details
```http
GET http://localhost:8080/api/orders/{id}
Authorization: Bearer {token}
```

### Update Order Status
```http
PUT http://localhost:8080/api/orders/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

**Allowed Status Values:**
- PENDING
- PROCESSING
- SHIPPED
- COMPLETED
- CANCELLED

---

## 3. Users API

### Get All Users
```http
GET http://localhost:8080/api/admin/users?size=50&page=0
Authorization: Bearer {token}
```

**Response Example:**
```json
{
  "content": [
    {
      "id": 1,
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "USER",
      "active": true,
      "phoneNumber": "+1-555-0123",
      "address": "123 Main St",
      "city": "New York",
      "country": "USA",
      "zipCode": "10001"
    }
  ],
  "pageable": {...},
  "totalElements": 45,
  "totalPages": 1
}
```

### Get User Details
```http
GET http://localhost:8080/api/admin/users/{id}
Authorization: Bearer {token}
```

### Update User
```http
PUT http://localhost:8080/api/admin/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith",
  "role": "ADMIN",
  "active": true
}
```

### Delete User
```http
DELETE http://localhost:8080/api/admin/users/{id}
Authorization: Bearer {token}
```

---

## 4. Admin Statistics API

### Get Dashboard Statistics
```http
GET http://localhost:8080/api/admin/statistics
Authorization: Bearer {token}
```

**Response Example:**
```json
{
  "totalProducts": 150,
  "totalOrders": 450,
  "totalUsers": 200,
  "totalRevenue": 45670.00,
  "activeOrders": 25,
  "lowStockProducts": 5
}
```

### Get Revenue by Period
```http
GET http://localhost:8080/api/admin/revenue?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {token}
```

**Response Example:**
```json
{
  "period": "January 2024",
  "totalRevenue": 45670.00,
  "totalOrders": 156,
  "averageOrderValue": 292.63
}
```

---

## 5. Cart API (For Logged-in Users)

### Get User Cart
```http
GET http://localhost:8080/api/cart
Authorization: Bearer {token}
```

**Response Example:**
```json
{
  "id": 1,
  "userId": 1,
  "items": [
    {
      "id": 1,
      "productId": 5,
      "productName": "Chanel No. 5",
      "price": 150.00,
      "quantity": 2
    }
  ],
  "totalPrice": 300.00,
  "totalItems": 2
}
```

### Add to Cart
```http
POST http://localhost:8080/api/cart/add
Authorization: Bearer {token}
Content-Type: application/json

{
  "productId": 5,
  "quantity": 1
}
```

### Remove from Cart
```http
DELETE http://localhost:8080/api/cart/remove/{cartItemId}
Authorization: Bearer {token}
```

---

## 6. Authentication API

### Login
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "admin123"
}
```

**Response Example:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN"
  }
}
```

### Register
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Refresh Token
```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

## Error Handling

### Common Error Responses

**401 Unauthorized (Invalid/Missing Token)**
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "status": 401
}
```

**403 Forbidden (Insufficient Permissions)**
```json
{
  "error": "Forbidden",
  "message": "Admin access required",
  "status": 403
}
```

**404 Not Found**
```json
{
  "error": "Not Found",
  "message": "Resource not found",
  "status": 404
}
```

**400 Bad Request**
```json
{
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "status": 400
}
```

**500 Internal Server Error**
```json
{
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "status": 500
}
```

---

## Pagination Parameters

Most endpoints support pagination:

```http
GET http://localhost:8080/api/products?page=0&size=20&sort=name,asc
```

- `page` - Zero-indexed page number (default: 0)
- `size` - Number of items per page (default: 20, max: 100)
- `sort` - Field,direction (e.g., name,asc or price,desc)

---

## Rate Limiting

- **Default limit:** 100 requests per minute
- **Header returned:** `X-RateLimit-Remaining`

---

## Status Codes Reference

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 204 | No Content - Successful, no response body |
| 400 | Bad Request - Invalid parameters |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 500 | Server Error - Internal server error |

---

## Using in Admin Panel

### Example: Fetch Products in Admin
```javascript
const fetchProducts = async () => {
  const token = localStorage.getItem('accessToken');
  const response = await fetch(
    'http://localhost:8080/api/products?size=100',
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  const data = await response.json();
  setProducts(data.content || []);
};
```

### Example: Update Product
```javascript
const updateProduct = async (productId, updates) => {
  const token = localStorage.getItem('accessToken');
  const response = await fetch(
    `http://localhost:8080/api/products/${productId}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updates)
    }
  );
  return response.json();
};
```

### Example: Delete User
```javascript
const deleteUser = async (userId) => {
  const token = localStorage.getItem('accessToken');
  const response = await fetch(
    `http://localhost:8080/api/admin/users/${userId}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return response.ok;
};
```

---

## Testing with curl

### Get Products
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/products?size=10
```

### Create Product
```bash
curl -X POST \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","price":100}' \
  http://localhost:8080/api/products
```

### Update Product
```bash
curl -X PUT \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated","price":120}' \
  http://localhost:8080/api/products/1
```

### Delete Product
```bash
curl -X DELETE \
  -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/products/1
```

---

## API Base URL Configuration

For different environments, update the base URL:

```javascript
// Development
const API_URL = 'http://localhost:8080';

// Production
const API_URL = 'https://api.perfumeshop.com';

// Staging
const API_URL = 'https://staging-api.perfumeshop.com';
```

---

## Best Practices

1. **Always include Authorization header**
   - Required for all protected endpoints

2. **Handle errors properly**
   - Check response status
   - Display user-friendly messages
   - Log errors for debugging

3. **Use pagination**
   - Don't fetch all records at once
   - Use page and size parameters
   - Implement infinite scroll or pagination UI

4. **Cache when appropriate**
   - Store non-changing data
   - Implement refresh mechanisms
   - Consider HTTP caching headers

5. **Validate input**
   - Validate on client side
   - Server will validate too
   - Provide feedback to users

---

**Last Updated:** January 2026
**API Version:** 1.0.0
