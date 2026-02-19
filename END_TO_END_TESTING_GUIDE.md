# End-to-End Testing Guide

## 🎯 Quick Start

**Status:**
- ✅ Frontend: http://localhost:3000 (RUNNING)
- ⏳ Backend: http://localhost:8080 (Building... Check Maven window)

**When backend is ready, proceed with testing below.**

---

## 🧪 Complete Test Scenarios

### Scenario 1: Guest User Browse & Register

**Steps:**
1. Visit http://localhost:3000
2. Browse products on home page
3. Click "Men" or "Women" category
4. Click on a product to view details
5. Click "Add to Cart" (redirects to register if not logged in)
6. Register new account:
   - Email: test@example.com
   - Password: Test123!
   - Name: Test User
7. Verify redirect back to products

**Expected:**
- ✅ Premium UI visible (Inter font, clean design)
- ✅ Product cards show badges and ratings
- ✅ Skeleton loaders during data fetch
- ✅ Registration successful
- ✅ JWT token stored (check browser DevTools > Application > Local Storage)

---

### Scenario 2: Login & Shopping Cart

**Steps:**
1. If registered in Scenario 1, logout first
2. Click "Login" in navbar
3. Use demo credentials (shown on login page):
   ```
   Email: john@example.com
   Password: password123
   ```
4. Verify successful login
5. Go to /products
6. Add 3-4 different products to cart
7. Check cart badge updates in navbar
8. Go to /cart
9. Test quantity controls (+/- buttons)
10. Remove one item
11. Verify subtotal calculations

**Expected:**
- ✅ Login successful with JWT
- ✅ Cart badge shows correct count
- ✅ Cart items display with images
- ✅ Quantity updates work smoothly
- ✅ Prices calculate correctly
- ✅ Remove button works

---

### Scenario 3: Checkout Flow (Demo Payment)

**Prerequisites:** Cart must have items from Scenario 2

**Steps:**
1. Click "Proceed to Checkout" in cart
2. Fill shipping information:
   ```
   Address: 123 Main Street, Apt 4B
   City: Mumbai
   Country: India
   Zip Code: 400001
   Phone: +919876543210
   ```
3. Verify order summary shows correct items and total
4. Look for amber "Demo Mode" banner
5. Click "Complete Demo Payment"
6. Wait 1.5 seconds for simulation
7. Check for success toast
8. Verify redirect to /orders/{id}
9. Check order details page:
   - Order number
   - Status: CONFIRMED
   - All items listed
   - Shipping info correct
   - Total amount correct

**Expected:**
- ✅ Checkout form validates inputs
- ✅ Demo mode banner visible
- ✅ Payment simulation completes
- ✅ Success toast appears
- ✅ Cart cleared after payment
- ✅ Order saved in database
- ✅ Stock deducted from products

---

### Scenario 4: Order History

**Steps:**
1. Stay logged in from Scenario 3
2. Click user icon in navbar
3. Select "My Orders" (or go to /orders)
4. View order from Scenario 3
5. Check order details
6. Try to cancel order (if implemented)

**Expected:**
- ✅ Order list shows all orders
- ✅ Order details accessible
- ✅ Status displayed correctly
- ✅ Items and totals match

---

### Scenario 5: Admin Panel Access

**Steps:**
1. Logout current user
2. Login with admin credentials:
   ```
   Email: admin@example.com
   Password: admin123
   ```
3. Click "Admin" in navbar (or go to /admin)
4. Navigate through admin sections:
   - Dashboard / Overview
   - Products Management
   - Orders Management
   - Users Management

**Test Product Management:**
5. Click "Add Product"
6. Create new product:
   ```
   Name: Test Perfume
   Brand: Test Brand
   Category: UNISEX
   Price: 999
   Stock: 10
   Description: Test product for testing
   ```
7. Save product
8. Verify it appears in product list
9. Edit the product
10. Delete the product

**Test Order Management:**
11. Go to Orders section
12. View order from Scenario 3
13. Try updating order status

**Expected:**
- ✅ Admin login successful
- ✅ Admin panel accessible
- ✅ Sidebar navigation works
- ✅ Product CRUD operations complete
- ✅ Order status updates work
- ✅ Clean, professional UI

---

### Scenario 6: Mobile Responsiveness

**Steps:**
1. Open browser DevTools (F12)
2. Toggle device toolbar (Ctrl+Shift+M)
3. Select "iPhone 12 Pro" or similar
4. Repeat Scenarios 1-3 on mobile view
5. Test:
   - Navigation menu (mobile)
   - Product cards (grid layout)
   - Cart (mobile optimized)
   - Checkout form (mobile)
   - Payment button (mobile)

**Expected:**
- ✅ All pages responsive
- ✅ Touch targets adequate size
- ✅ No horizontal scrolling
- ✅ Forms usable on mobile
- ✅ Images scale properly

---

### Scenario 7: Error Handling

**Test Network Errors:**
1. Open DevTools > Network tab
2. Set throttling to "Offline"
3. Try to load products
4. Verify error alert appears
5. Set back to "Online"
6. Verify data loads

**Test Empty States:**
7. Create new user account
8. Go to /cart (should be empty)
9. Verify empty cart message
10. Go to /orders (should be empty)
11. Verify empty orders message

**Test Validation:**
12. Go to checkout with empty cart
13. Verify redirect to /cart
14. Add items, go to checkout
15. Try submitting with empty fields
16. Verify validation messages

**Expected:**
- ✅ Network errors show user-friendly alerts
- ✅ Empty states have helpful messages
- ✅ Form validation works
- ✅ No crashes or white screens

---

### Scenario 8: Payment Integration (When Razorpay Configured)

**Note:** Only test if Razorpay live/test keys are configured

**Steps:**
1. Complete Scenarios 1-2
2. Go to checkout
3. Fill shipping info
4. Verify NO amber "Demo Mode" banner
5. Click "Pay with Razorpay"
6. Razorpay modal should open
7. Use test cards:
   ```
   Card: 4111 1111 1111 1111
   CVV: Any 3 digits
   Expiry: Any future date
   ```
8. Complete payment in Razorpay modal
9. Verify payment verification
10. Check order confirmation

**Expected:**
- ✅ Razorpay modal opens
- ✅ Real payment processing
- ✅ Signature verification succeeds
- ✅ Order confirmed
- ✅ Stock deducted

---

## 🔍 Things to Look For

### UI/UX Quality Checks
- ✅ **Design System Consistency:** Same fonts, colors, spacing everywhere
- ✅ **Loading States:** Skeleton loaders, no jumping content
- ✅ **Button States:** Hover effects, disabled states, loading spinners
- ✅ **Form UX:** Clear labels, validation messages, placeholder text
- ✅ **Icons:** Lucide icons used consistently
- ✅ **Cards:** Consistent shadow, border, padding
- ✅ **Spacing:** Generous whitespace, not cramped
- ✅ **Typography:** Clear hierarchy, readable

### Functionality Checks
- ✅ **Cart Persistence:** Cart saved across page refreshes
- ✅ **Authentication:** JWT token persists, auto-logout on expire
- ✅ **Stock Updates:** Products reflect correct stock after orders
- ✅ **Price Calculations:** Tax, shipping, discounts correct
- ✅ **Order Status:** Status updates reflect correctly
- ✅ **Search/Filter:** Products filter correctly

### Performance Checks
- ✅ **Page Load:** Initial load under 3 seconds
- ✅ **API Calls:** Response times under 500ms
- ✅ **Image Loading:** Images lazy load, proper sizing
- ✅ **No Memory Leaks:** No console errors accumulating
- ✅ **Smooth Transitions:** Animations smooth, no jank

---

## 🐛 Bug Report Template

If you find issues, document them like this:

```markdown
### Bug: [Short description]

**Severity:** Critical / High / Medium / Low

**Steps to Reproduce:**
1. Step one
2. Step two
3. Step three

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Screenshots:**
[Attach if relevant]

**Environment:**
- Browser: Chrome 120
- Device: Desktop / Mobile
- Screen Size: 1920x1080

**Console Errors:**
[Copy any errors from browser console]
```

---

## ✅ Test Completion Checklist

After completing all scenarios:

- [ ] Scenario 1: Guest Browse & Register
- [ ] Scenario 2: Login & Shopping Cart
- [ ] Scenario 3: Checkout (Demo Payment)
- [ ] Scenario 4: Order History
- [ ] Scenario 5: Admin Panel
- [ ] Scenario 6: Mobile Responsiveness
- [ ] Scenario 7: Error Handling
- [ ] Scenario 8: Real Payment (if configured)

**UI Quality:**
- [ ] Design system consistent across all pages
- [ ] Loading states prevent layout shifts
- [ ] Error messages clear and helpful
- [ ] Mobile experience excellent
- [ ] No console errors

**Functionality:**
- [ ] Authentication works smoothly
- [ ] Cart operations reliable
- [ ] Checkout flow complete
- [ ] Payment processing (demo) works
- [ ] Admin panel fully functional
- [ ] Stock management accurate

**Performance:**
- [ ] Pages load quickly
- [ ] No janky animations
- [ ] API responses fast
- [ ] No memory leaks

---

## 📊 Test Results Summary

**Date:** _____________  
**Tester:** _____________  
**Environment:** Development / Staging / Production

### Overall Status
- [ ] ✅ All tests passed
- [ ] ⚠️ Minor issues found (document below)
- [ ] ❌ Critical issues found (document below)

### Issues Found
1. 
2. 
3. 

### Recommendations
1. 
2. 
3. 

### Sign-off
**Ready for Production:** Yes / No / With Fixes

---

## 🚀 Quick Commands

```bash
# Check services
curl http://localhost:8080/actuator/health
curl http://localhost:3000

# View backend logs
# Check the PowerShell window running Maven

# Build production frontend
cd frontend
npm run build

# Build production backend
mvn clean package -DskipTests

# Run tests
cd frontend && npm test
cd backend && mvn test
```

---

**Next Steps After Testing:**
1. Document any bugs found
2. Fix critical issues
3. Review [DEPLOYMENT_READINESS_CHECKLIST.md](DEPLOYMENT_READINESS_CHECKLIST.md)
4. Plan production deployment
5. Set up Razorpay live keys
6. Deploy to staging first

---

**Status:** Ready for comprehensive testing!  
**Backend:** Wait for Maven build to complete, then test end-to-end.
