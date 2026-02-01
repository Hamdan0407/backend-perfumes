# Perfume Shop - User & Admin Guide
## Easy-to-Understand Documentation for Everyone

---

## 📖 Quick Navigation
- [What is Perfume Shop?](#what-is-perfume-shop)
- [Getting Started](#getting-started)
- [For Customers](#for-customers)
- [For Admins](#for-admins)
- [Common Questions](#common-questions)
- [Need Help?](#need-help)

---

## 🎯 What is Perfume Shop?

**Perfume Shop** is an online store where people can:
- 👃 **Browse** different perfumes
- 🛒 **Add items** to their shopping cart
- 💳 **Pay** securely online
- 📦 **Track** their orders
- ⭐ **Review** products they bought

Think of it like **Amazon but just for perfumes!**

---

## ✅ Getting Started

### Step 1: Open the Website
Just open your web browser and go to:
```
http://localhost:3000
```

You'll see the Perfume Shop homepage with:
- 🏠 **Home Page** - Welcome & featured perfumes
- 👃 **Shop** - Browse all perfumes
- 🛒 **Cart** - Your selected items
- 👤 **Account** - Your profile & orders

### Step 2: Create Your Account (Free!)
**Click on "Register"** in the top menu and fill in:
- **Username** - Choose your unique name (e.g., "PerfumeGuy2024")
- **Email** - Your email address
- **Password** - A strong password (mix of letters, numbers)

✅ **Done!** You're now a member!

### Step 3: Login
**Click "Login"** and enter your email and password.

---

## 🛍️ For Customers

### How to Shop

#### **1. Find Perfumes**
- Click on **"Shop"** or **"Products"**
- You'll see a list of perfumes with:
  - 🖼️ **Picture** of the bottle
  - 💰 **Price** in your currency
  - 📝 **Description** - What it smells like
  - ⭐ **Reviews** - What other customers say

#### **2. Learn More About a Perfume**
Click on any perfume to see:
- Full description
- All customer reviews & ratings (1-5 stars)
- Price & availability
- Similar products

#### **3. Add to Cart**
1. Click the **"Add to Cart"** button
2. Choose how many bottles you want
3. Click **"Add"**

You'll see a message: "✅ Added to cart!"

#### **4. Review Your Cart**
Click the **🛒 Cart** icon (top right) to see:
- All items you selected
- Price of each item
- Total cost
- Button to **"Proceed to Checkout"**

#### **5. Checkout (Buying)**
1. Click **"Proceed to Checkout"**
2. Confirm your **delivery address**
3. Choose your **payment method**:
   - 💳 Credit/Debit Card (Stripe)
   - 🏦 Bank Transfer (Razorpay)
4. Complete the payment
5. 🎉 **Order placed!**

#### **6. Track Your Order**
Go to **"My Orders"** to see:
- Order number
- What you ordered
- Current status:
  - 📋 **Pending** - We're preparing
  - ✅ **Confirmed** - Payment received
  - 🚚 **Shipped** - On the way
  - 📦 **Delivered** - You got it!
  - ❌ **Cancelled** - Order cancelled

#### **7. Write a Review**
After receiving your order:
1. Go to **"My Orders"**
2. Click on the order
3. Click **"Write Review"**
4. Give a rating (1-5 stars ⭐)
5. Write your opinion (optional)
6. Submit!

### Managing Your Account

#### **Update Profile**
Click **👤 Account** → **Profile Settings**
- Change password
- Update email
- Add delivery addresses

#### **View Order History**
Click **👤 Account** → **My Orders**
- See all past orders
- Download receipts
- Reorder same items

#### **Wishlist** (Save for Later)
Click the ❤️ heart icon on any product to:
- Save it for later
- Get price notifications
- Share with friends

---

## 🔑 For Admins

### Admin Login
Use these credentials:
- **Email:** `admin@perfumeshop.local`
- **Password:** `Admin123`

⚠️ **IMPORTANT:** Change this password immediately after first login!

### Admin Dashboard
After logging in, click **"Admin Panel"** to access:

#### **1. Dashboard (Overview)**
See at a glance:
- 📊 **Total Sales** this month
- 📦 **Total Orders**
- 👥 **New Customers**
- 💰 **Revenue**

#### **2. Products Management**

##### **Add New Perfume**
1. Click **"Add Product"**
2. Fill in:
   - 📝 **Name** - Product name
   - 📄 **Description** - What it smells like, ingredients, etc.
   - 💰 **Price** - Cost in currency
   - 📦 **Stock** - How many bottles you have
   - 🖼️ **Image** - Upload a photo
   - 🏷️ **Category** - Type (Eau de Parfum, Cologne, etc.)
3. Click **"Save"**

##### **Edit Perfume**
1. Find the product in the product list
2. Click **"Edit"**
3. Change information as needed
4. Click **"Save"**

##### **Delete Perfume**
1. Find the product
2. Click **"Delete"**
3. Confirm deletion

#### **3. Orders Management**

##### **View All Orders**
Click **"Orders"** to see all customer purchases:
- Order number
- Customer name
- Order date
- Current status
- Total amount

##### **Update Order Status**
1. Click on an order
2. Change the status:
   - 📋 **Pending** → When just placed
   - ✅ **Confirmed** → Payment confirmed
   - 🚚 **Shipped** → Sent to customer
   - 📦 **Delivered** → Customer received
   - ❌ **Cancelled** → If needed
3. Click **"Save"**

💡 **Tip:** Customer gets email notification with each status change!

#### **4. Customers Management**

##### **View All Customers**
Click **"Customers"** to see:
- Customer names & emails
- Number of orders
- Total spent
- Join date

##### **View Customer Details**
Click on a customer name to see:
- Contact information
- All their orders
- Reviews they wrote
- Account status

##### **Manage Roles** (Optional)
Make someone an admin:
1. Find the customer
2. Click **"Change Role"**
3. Select "Admin"
4. They can now manage the shop!

#### **5. Reviews Moderation**

##### **View All Reviews**
Click **"Reviews"** to see:
- Customer who wrote it
- Rating given (1-5 stars)
- What they wrote
- Product reviewed

##### **Remove Inappropriate Reviews**
If a review is rude or inappropriate:
1. Click on the review
2. Click **"Delete"**
3. It's removed from the site

---

## ❓ Common Questions

### Customer Questions

**Q: Is my payment safe?**
A: Yes! We use Stripe and Razorpay, the safest payment systems. Your card details are encrypted (protected).

**Q: How long does delivery take?**
A: Usually 3-7 business days. You can track your order status anytime.

**Q: Can I return items?**
A: Check our return policy. Most unopened perfumes can be returned within 30 days.

**Q: What if I forget my password?**
A: Click **"Forgot Password?"** on the login page. We'll send you a reset link to your email.

**Q: Can I change my order after placing it?**
A: Contact support quickly if order hasn't shipped yet. Once shipped, you'll need to refuse delivery or use our return policy.

**Q: Do you ship internationally?**
A: Currently, we ship to [Your Country]. Check shipping page for details.

---

### Admin Questions

**Q: How do I backup the data?**
A: Contact your IT team. They handle automatic daily backups.

**Q: Can multiple people be admins?**
A: Yes! Go to "Customers" → Find person → "Change Role" → "Admin"

**Q: What if someone writes a fake review?**
A: You can delete it from the Reviews section. Consider setting up review moderation.

**Q: How do I see sales reports?**
A: Go to Dashboard. You can see:
- Daily/weekly/monthly sales
- Best-selling products
- Revenue trends

**Q: What's the difference between "Pending" and "Confirmed"?**
- **Pending** = Order just placed, payment not yet verified
- **Confirmed** = Payment received, order is ready to ship

---

## 🚀 How It Works (Behind the Scenes)

Don't worry about these details, but here's what happens:

1. **You browse perfumes** → Website shows products from database
2. **You add to cart** → Information saved temporarily
3. **You checkout** → Payment goes to Stripe/Razorpay (secure)
4. **Order is placed** → Admin notified, database updated
5. **Admin confirms** → Warehouse prepares order
6. **Order ships** → Delivery company takes it
7. **You receive** → You get your perfume!
8. **You review** → Feedback stored for others to see

---

## 💡 Tips & Tricks

### For Customers
- 💬 **Read reviews** before buying - see what others think
- 🎁 **Add to wishlist** items you like but can't afford now
- 📧 **Check your email** for order updates
- 💰 **Compare prices** - we show similar products
- ⭐ **Leave honest reviews** - help other customers decide

### For Admins
- 📊 **Check dashboard daily** - stay updated on sales
- 🔐 **Change your password regularly** - keep account safe
- 📧 **Respond to customer emails** - good customer service
- 🗑️ **Remove spam reviews** - keep site clean
- 📦 **Update order status promptly** - customers appreciate updates
- 📸 **Use good product photos** - nice images = more sales

---

## 🆘 Need Help?

### Customer Support
**Email:** support@perfumeshop.local
**Hours:** Mon-Fri 9AM-6PM
**Response Time:** Usually within 24 hours

### Common Problems & Quick Fixes

#### **Problem: Can't Login**
- ✅ Check CAPS LOCK is OFF
- ✅ Verify email is correct
- ✅ Click "Forgot Password" to reset

#### **Problem: Product Not Showing**
- ✅ Refresh page (press F5)
- ✅ Clear browser cache
- ✅ Try different browser

#### **Problem: Payment Failed**
- ✅ Check internet connection
- ✅ Verify card details are correct
- ✅ Try again in a few minutes
- ✅ Contact your bank

#### **Problem: Can't Find Order**
- ✅ Go to Account → My Orders
- ✅ Look for order number in email
- ✅ Check if it was cancelled

#### **Problem: Review Not Posting**
- ✅ Refresh the page
- ✅ Check if you already reviewed this product
- ✅ Make sure review isn't too long

---

## 📞 Admin Emergency Contact

If something is broken:
1. **Check if service is running:** http://localhost:8080/api/products
2. **Restart services:** `docker compose restart`
3. **Contact IT:** Your technical team

---

## 🎓 Learning Resources

### Useful Links
- 📚 Full technical documentation (for developers)
- 🎥 Video tutorials (coming soon)
- 📱 Mobile app (coming soon)
- 🌐 Social media: @PerfumeShop

### Training for Admins
1. **Week 1:** Learn basic functions (products, orders)
2. **Week 2:** Learn advanced features (customers, analytics)
3. **Week 3:** Learn reporting and data analysis

---

## 📋 Checklist for Launch

### Before Going Live
- [ ] Change admin default password
- [ ] Add at least 10 sample perfumes
- [ ] Test checkout process (use test card)
- [ ] Verify payment gateways work
- [ ] Check all product images load
- [ ] Test on mobile phone
- [ ] Write privacy policy
- [ ] Set up email notifications
- [ ] Create refund/return policy
- [ ] Train all admins

### Regular Maintenance
- [ ] Update products weekly
- [ ] Check orders daily
- [ ] Respond to reviews
- [ ] Backup data weekly
- [ ] Update software monthly
- [ ] Review analytics monthly

---

## 📊 Success Metrics to Track

### What to Monitor
- **Daily Orders:** How many orders per day
- **Total Revenue:** Money earned
- **Conversion Rate:** % of visitors who buy
- **Customer Rating:** Average review score
- **Product Views:** Which perfumes are popular
- **Cart Abandonment:** How many people don't complete purchase

### Setting Goals
- 📈 Increase sales by 20% this month
- ⭐ Maintain 4.5+ star rating
- 📦 Ship 95%+ orders on time
- 💬 Respond to customer emails within 24 hours

---

## 🎉 Congratulations!

You now understand how Perfume Shop works!

**Next Steps:**
1. ✅ Create your account (or login as admin)
2. ✅ Explore the website
3. ✅ Browse some perfumes
4. ✅ Try adding to cart
5. ✅ Make a test purchase

**Questions?** Check this guide again or contact support!

---

## 📝 Document Info

- **Created:** January 23, 2026
- **Version:** 1.0 (Non-Technical)
- **Language:** Simple English
- **For:** Everyone (non-technical users)
- **Status:** Ready to Use ✅

---

**Thank you for using Perfume Shop! 👃💕**

*Last updated: January 23, 2026*
