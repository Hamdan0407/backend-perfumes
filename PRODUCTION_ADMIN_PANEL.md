# Production-Grade Admin Panel

## Overview
A complete, production-ready admin dashboard for managing the Perfume Shop.

## Features

### 1. Dashboard
- Real-time statistics
- Total products count
- Total orders count
- Total users count
- Revenue tracking

### 2. Products Management
- View all products with pagination
- Display product details (name, category, price, stock, rating)
- Edit product functionality
- Delete product functionality
- Add new products

### 3. Orders Management
- View all orders with customer information
- Display order status with badge system
- Order date and amount tracking
- View detailed order information
- Multiple order statuses: Pending, Processing, Shipped, Completed

### 4. Users Management
- View all registered users
- Display user roles and status
- User email and name
- Edit user details
- Active/Inactive status tracking

### 5. Settings
- Store configuration
- Store name and email
- Support contact information
- Currency selection

## Access & Security

### Admin Access
- Only users with `ADMIN` role can access the admin panel
- Route protection via `PrivateRoute` component
- Token-based authentication
- Automatic logout on token expiration

### Login Credentials (For Testing)
**Email:** admin@example.com
**Password:** admin123

## API Integration

The admin panel integrates with the following endpoints:

### Products
```
GET http://localhost:8080/api/products?size=100
Headers: Authorization: Bearer {token}
```

### Orders
```
GET http://localhost:8080/api/orders?size=50
Headers: Authorization: Bearer {token}
```

### Users
```
GET http://localhost:8080/api/admin/users?size=50
Headers: Authorization: Bearer {token}
```

## UI/UX Features

### Responsive Design
- Desktop: Full sidebar navigation with 260px width
- Tablet: Collapsible sidebar
- Mobile: Horizontal navigation

### Visual Enhancements
- Gradient purple theme (consistent with app branding)
- Icon-based navigation with hover effects
- Data tables with striped rows
- Smooth animations and transitions
- Loading states and error handling

### Navigation
- Sidebar with collapsible menu
- Tab-based section switching
- Quick access buttons
- Status badges with color coding

## Component Structure

```
src/
├── pages/
│   └── AdminPanel.jsx           # Main admin component
├── styles/
│   └── AdminPanel.css           # Complete styling
└── App.jsx                       # Route configuration
```

## File Locations

- **Component:** [src/pages/AdminPanel.jsx](src/pages/AdminPanel.jsx)
- **Styles:** [src/styles/AdminPanel.css](src/styles/AdminPanel.css)
- **Routes:** Updated in [src/App.jsx](src/App.jsx)
- **Navbar:** Updated [src/components/Navbar.jsx](src/components/Navbar.jsx)

## How to Access

1. **Login with admin account:**
   - Email: admin@example.com
   - Password: admin123

2. **Navigate to admin panel:**
   - Click "Admin" link in navbar (appears for admins only)
   - Or go to: `http://localhost:3000/admin`

3. **Use the admin features:**
   - Click tabs on the left sidebar
   - View, edit, delete products
   - Manage orders and users
   - Configure settings

## Error Handling

- Failed API calls display error banners
- Loading states prevent UI blocking
- Empty states show helpful messages
- Invalid authentication redirects to login

## Production Checklist

- ✅ Role-based access control (RBAC)
- ✅ Token-based authentication
- ✅ Error handling and fallbacks
- ✅ Responsive design
- ✅ Professional UI/UX
- ✅ API integration
- ✅ Data tables with sorting capability
- ✅ Status badges and indicators
- ✅ Navigation with icons
- ✅ Settings management
- ✅ Logout functionality
- ✅ Empty/Loading states

## Future Enhancements

- Analytics dashboard with charts
- Product inventory alerts
- Order tracking timeline
- User activity logs
- Export functionality (CSV, PDF)
- Bulk actions
- Advanced filtering
- Search functionality
- Notification system

## Security Notes

- All routes are protected with `PrivateRoute`
- Admin role is required for access
- Tokens are validated on each request
- Logout clears all stored authentication data
- API calls include Bearer token in headers
