# Quick Testing Guide - Admin Panel

## Prerequisites
- All containers running: `docker compose ps`
- Access: http://localhost:3000

---

## Step 1: Login to Admin Account

1. Navigate to http://localhost:3000/login
2. Enter credentials:
   - **Email**: `admin2@test.com`
   - **Password**: `Test@1234`
3. Click "Sign In"
4. You should be redirected to home page
5. Click "Admin" link in navbar (top right)

---

## Step 2: Test Product Management

### Add a New Product:
1. Go to Admin > Products tab
2. Click "Add Product" button
3. Fill in the form:
   ```
   Product Name: Vanilla Sky Perfume
   Brand: Guerlain
   Category: Women
   Price: 4500
   Stock: 15
   Description: A beautiful vanilla-scented perfume
   Image URL: (leave blank for placeholder)
   ```
4. Click "Add Product"
5. ✅ Success! You should see:
   - Toast message: "Product added successfully!"
   - Product appears in the table below
   - List shows updated product count

### Delete a Product:
1. In Products table, click trash icon on any product
2. Confirm deletion in popup
3. ✅ Product removed from list
4. Toast message: "Product deleted successfully"

---

## Step 3: Test User Management

### Block a User:
1. Go to Admin > Users tab
2. Look for any user card (e.g., "John Doe")
3. Click "Block User" button
4. ✅ User status changes to "Blocked"
5. Toast message: "User blocked successfully"
6. User card shows with blocked styling

### Unblock a User:
1. Filter by "Blocked" status
2. Click "Unblock User" on blocked user
3. ✅ User status changes to "Active"
4. Toast message: "User unblocked successfully"
5. Can filter again and see in "Active" users

### Filter Users:
1. Click "Active" button - see only active users
2. Click "Blocked" button - see only blocked users
3. Click "All" button - see everyone

---

## Step 4: Test Dashboard & Statistics

### View Dashboard:
1. Go to Admin > Dashboard tab
2. You should see 4 stat cards:
   - **Total Revenue** (₹ in INR)
   - **Total Orders** (number of orders)
   - **Total Users** (number of users)
   - **Total Products** (number of products)

### View Quick Stats:
1. Scroll down to "Quick Stats" section
2. See:
   - Average Order Value (calculated)
   - Orders Processed
   - Active Products
   - Registered Users

### Refresh Stats:
1. Click the refresh icon next to "Dashboard Statistics"
2. Wait for stats to update
3. Check "Last updated" timestamp
4. Stats auto-refresh every 60 seconds

---

## Step 5: Test Orders Management

### View Orders:
1. Go to Admin > Orders tab
2. See all orders in table format
3. Each row shows:
   - Order number
   - Customer name
   - Number of items
   - Amount in ₹ (INR)
   - Status (Pending, Processing, Shipped, etc.)
   - Date

### Filter Orders by Status:
1. Click "All" - see all orders
2. Click "Pending" - see pending orders
3. Click "Processing" - see processing orders
4. Click "Shipped" - see shipped orders
5. Click "Delivered" - see delivered orders

### Order Auto-Refresh:
1. Orders list auto-refreshes every 30 seconds
2. New orders appear automatically

---

## Expected Results

### ✅ Success Indicators:

1. **Product Adding Works**
   - Modal opens and closes properly
   - Form validates required fields
   - Product appears in table after submission
   - Toast notifications show success/error

2. **User Blocking Works**
   - Block/Unblock buttons toggle status
   - Status visible on user card
   - Filtering works by status
   - Toast notifications appear

3. **Dashboard Stats Load**
   - All 4 stat cards display numbers
   - Stats are not all zeros
   - Refresh button works
   - Last updated timestamp changes
   - Auto-refresh happens (watch timestamps)

4. **Orders Display Correctly**
   - All orders show in table
   - Amounts display in ₹ (not $)
   - Filtering by status works
   - Dates are formatted properly

5. **Error Handling Works**
   - Try adding product with empty fields - should show validation error
   - Try blocking a user - should show success toast
   - Check browser console for any errors (should be minimal)

---

## Common Issues & Solutions

### Issue: "Failed to load" messages
- **Solution**: Check if backend API is running
  ```bash
  docker compose ps
  # Should show all 3 containers as "Running"
  ```

### Issue: Login doesn't work
- **Solution**: Verify credentials are exactly:
  - Email: `admin2@test.com`
  - Password: `Test@1234`

### Issue: Prices show in $ instead of ₹
- **Solution**: This has been fixed. If still seeing $, refresh page.

### Issue: Products/Users list shows "No items found"
- **Solution**: This means the API returned empty list (possible but normal if you just started)

### Issue: "Authorization failed" errors
- **Solution**: 
  1. Check if token is stored in localStorage
  2. Try logging out and back in
  3. Clear browser cache

---

## Performance Notes

- **Product List**: Loads up to 100 products with pagination
- **Users List**: Shows up to 50 users
- **Orders**: Shows up to 50 recent orders
- **Auto-Refresh**: Orders every 30 seconds, Stats every 60 seconds
- **Load Time**: Most operations should complete in <2 seconds

---

## Test Data

### Pre-loaded Users:
- Email: `admin2@test.com` - Role: ADMIN
- Additional users may exist in database

### Pre-loaded Products:
- 20 perfume products should be loaded
- Categories: Women, Men, Unisex
- Prices in INR (₹)

---

## Admin Credentials
```
Email: admin2@test.com
Password: Test@1234
Role: ADMIN
Status: Active
```

---

## Contact/Issues
If you encounter any issues:
1. Check browser console (F12) for errors
2. Check Docker logs: `docker compose logs api`
3. Verify all containers are running
4. Try clearing browser cache and logging in again

---

**Last Updated**: January 24, 2026  
**Test Version**: 2.0  
✅ All features working and optimized
