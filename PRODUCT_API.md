# Product Management API Documentation

## Public Product Endpoints

### 1. Get All Products
```http
GET /api/products?page=0&size=12&sortBy=createdAt&sortDir=DESC
```
**Description:** Get paginated list of all active products  
**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Items per page (default: 12)
- `sortBy` (optional): Sort field (name, price, rating, createdAt) (default: createdAt)
- `sortDir` (optional): Sort direction (ASC, DESC) (default: DESC)

**Response:** Page<ProductResponse>

---

### 2. Get Product by ID
```http
GET /api/products/{id}
```
**Description:** Get single product details  
**Response:** ProductResponse

---

### 3. Get Products by Category
```http
GET /api/products/category/{category}?page=0&size=12&sortBy=createdAt&sortDir=DESC
```
**Description:** Get products filtered by category (Men, Women, Unisex)  
**Path Parameters:**
- `category`: Product category

**Response:** Page<ProductResponse>

---

### 4. Get Products by Brand
```http
GET /api/products/brand/{brand}?page=0&size=12&sortBy=createdAt&sortDir=DESC
```
**Description:** Get products filtered by brand  
**Response:** Page<ProductResponse>

---

### 5. Get Featured Products
```http
GET /api/products/featured
```
**Description:** Get all featured products (no pagination)  
**Response:** List<ProductResponse>

---

### 6. Search Products
```http
GET /api/products/search?query=chanel&page=0&size=12&sortBy=createdAt&sortDir=DESC
```
**Description:** Search products by name, brand, or description  
**Query Parameters:**
- `query` (required): Search query string

**Response:** Page<ProductResponse>

---

### 7. Get Products by Price Range
```http
GET /api/products/price-range?minPrice=50.00&maxPrice=200.00&page=0&size=12
```
**Description:** Filter products by price range  
**Query Parameters:**
- `minPrice` (required): Minimum price
- `maxPrice` (required): Maximum price

**Response:** Page<ProductResponse>

---

### 8. Advanced Filter Products
```http
POST /api/products/filter
Content-Type: application/json

{
  "searchQuery": "perfume",
  "category": "Women",
  "brands": ["Chanel", "Dior"],
  "minPrice": 50.00,
  "maxPrice": 300.00,
  "featured": true,
  "minRating": 4,
  "inStock": true,
  "page": 0,
  "size": 12,
  "sortBy": "price",
  "sortDir": "ASC"
}
```
**Description:** Advanced filtering with multiple criteria  
**Request Body:** ProductFilterRequest (all fields optional)  
**Response:** Page<ProductResponse>

---

### 9. Get All Brands
```http
GET /api/products/brands
```
**Description:** Get list of all available brands  
**Response:** List<String>

---

### 10. Get All Categories
```http
GET /api/products/categories
```
**Description:** Get list of all available categories  
**Response:** List<String>

---

## Admin Product Endpoints
**All admin endpoints require `ROLE_ADMIN` and are prefixed with `/api/admin`**

### 11. Get All Products (Admin)
```http
GET /api/admin/products?page=0&size=20&sortBy=createdAt&sortDir=DESC
Authorization: Bearer <admin_token>
```
**Description:** Get all products including inactive ones  
**Response:** Page<ProductResponse>

---

### 12. Get Products by Status
```http
GET /api/admin/products/status?active=true&page=0&size=20
Authorization: Bearer <admin_token>
```
**Description:** Filter products by active status  
**Query Parameters:**
- `active` (required): true or false

**Response:** Page<ProductResponse>

---

### 13. Get Product by ID (Admin)
```http
GET /api/admin/products/{id}
Authorization: Bearer <admin_token>
```
**Description:** Get product details including inactive products  
**Response:** ProductResponse

---

### 14. Create Product
```http
POST /api/admin/products
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "name": "Chanel No. 5",
  "brand": "Chanel",
  "description": "Iconic perfume with floral notes",
  "price": 150.00,
  "discountPrice": 135.00,
  "stock": 50,
  "category": "Women",
  "type": "Eau de Parfum",
  "volume": 100,
  "imageUrl": "https://example.com/chanel-no5.jpg",
  "additionalImages": [
    "https://example.com/chanel-no5-2.jpg"
  ],
  "fragranceNotes": ["Rose", "Jasmine", "Ylang-Ylang"],
  "featured": true,
  "active": true
}
```
**Description:** Create new product  
**Request Body:** ProductRequest (validated)  
**Response:** ProductResponse (HTTP 201 Created)

**Validation Rules:**
- name: 3-200 characters, required
- brand: max 100 characters, required
- description: 10-2000 characters, required
- price: > 0, required
- discountPrice: optional, must be < price
- stock: >= 0, required
- category: Must be "Men", "Women", or "Unisex"
- volume: 1-1000 ml

---

### 15. Update Product (Full)
```http
PUT /api/admin/products/{id}
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "name": "Updated Product Name",
  "brand": "Updated Brand",
  ...all fields required...
}
```
**Description:** Full update of product (all fields required)  
**Request Body:** ProductRequest  
**Response:** ProductResponse

---

### 16. Update Product (Partial)
```http
PATCH /api/admin/products/{id}
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "price": 175.00,
  "stock": 25
}
```
**Description:** Partial update (only provided fields updated)  
**Request Body:** ProductRequest (all fields optional)  
**Response:** ProductResponse

---

### 17. Soft Delete Product
```http
DELETE /api/admin/products/{id}
Authorization: Bearer <admin_token>
```
**Description:** Deactivate product (set active = false)  
**Response:** ApiResponse

---

### 18. Permanent Delete Product
```http
DELETE /api/admin/products/{id}/permanent
Authorization: Bearer <admin_token>
```
**Description:** Permanently delete product from database  
**⚠️ Warning:** This action cannot be undone  
**Response:** ApiResponse

---

### 19. Activate Product
```http
PATCH /api/admin/products/{id}/activate
Authorization: Bearer <admin_token>
```
**Description:** Activate deactivated product  
**Response:** ProductResponse

---

### 20. Deactivate Product
```http
PATCH /api/admin/products/{id}/deactivate
Authorization: Bearer <admin_token>
```
**Description:** Deactivate product  
**Response:** ProductResponse

---

### 21. Toggle Featured Status
```http
PATCH /api/admin/products/{id}/featured
Authorization: Bearer <admin_token>
```
**Description:** Toggle featured flag (true ↔ false)  
**Response:** ProductResponse

---

### 22. Update Stock (Absolute)
```http
PATCH /api/admin/products/{id}/stock?quantity=100
Authorization: Bearer <admin_token>
```
**Description:** Set product stock to specific value  
**Query Parameters:**
- `quantity` (required): New stock quantity (>= 0)

**Response:** ProductResponse

---

### 23. Adjust Stock (Relative)
```http
PATCH /api/admin/products/{id}/stock/adjust?adjustment=-5
Authorization: Bearer <admin_token>
```
**Description:** Add or subtract from current stock  
**Query Parameters:**
- `adjustment` (required): Amount to add (positive) or subtract (negative)

**Example:**
- Current stock: 50
- Adjustment: -5
- New stock: 45

**Response:** ProductResponse

---

### 24. Get Low Stock Products
```http
GET /api/admin/products/low-stock?threshold=10
Authorization: Bearer <admin_token>
```
**Description:** Get products with stock below threshold  
**Query Parameters:**
- `threshold` (optional): Stock threshold (default: 10)

**Response:** List<ProductResponse>

---

### 25. Get Product Statistics
```http
GET /api/admin/products/statistics
Authorization: Bearer <admin_token>
```
**Description:** Get comprehensive product statistics  
**Response:**
```json
{
  "totalActiveProducts": 145,
  "totalOutOfStockProducts": 8,
  "totalBrands": 25,
  "totalCategories": 3,
  "brands": ["Chanel", "Dior", "Gucci", ...],
  "categories": ["Men", "Women", "Unisex"]
}
```

---

## Response DTOs

### ProductResponse
```json
{
  "id": 1,
  "name": "Chanel No. 5",
  "brand": "Chanel",
  "description": "Iconic perfume with floral notes",
  "price": 150.00,
  "discountPrice": 135.00,
  "stock": 50,
  "category": "Women",
  "type": "Eau de Parfum",
  "volume": 100,
  "imageUrl": "https://example.com/chanel-no5.jpg",
  "additionalImages": ["https://example.com/chanel-no5-2.jpg"],
  "fragranceNotes": ["Rose", "Jasmine", "Ylang-Ylang"],
  "featured": true,
  "active": true,
  "rating": 4.8,
  "reviewCount": 245,
  "createdAt": "2026-01-18T10:30:00",
  "updatedAt": "2026-01-18T14:20:00"
}
```

### Page Response
```json
{
  "content": [...array of ProductResponse...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 12,
    "sort": { "sorted": true, "unsorted": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 150,
  "totalPages": 13,
  "last": false,
  "first": true,
  "size": 12,
  "number": 0,
  "numberOfElements": 12,
  "empty": false
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "price": "Price must be greater than 0",
    "name": "Product name is required"
  }
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access Denied",
  "data": null
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Product not found with id: 123",
  "data": null
}
```

---

## Features Summary

### Public Features
✅ Pagination & sorting  
✅ Category & brand filtering  
✅ Price range filtering  
✅ Text search (name, brand, description)  
✅ Advanced multi-criteria filtering  
✅ Featured products listing  
✅ Get available brands & categories  

### Admin Features
✅ Full CRUD operations (Create, Read, Update, Delete)  
✅ Soft delete & permanent delete  
✅ Activate/deactivate products  
✅ Toggle featured status  
✅ Absolute & relative stock management  
✅ Low stock alerts  
✅ Product statistics dashboard  
✅ View inactive products  
✅ Partial updates (PATCH)  
✅ Comprehensive validation  

### Performance Optimizations
✅ Database indexes on frequently queried columns  
✅ Lazy loading for relationships  
✅ DTO pattern to avoid entity exposure  
✅ Optimized JPQL queries  
✅ Transaction management  
✅ Logging for audit trails  
