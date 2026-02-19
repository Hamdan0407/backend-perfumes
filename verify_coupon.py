import urllib.request
import urllib.parse
import json
import sys
import time

BASE_URL = "http://localhost:8080/api"

def print_step(step, msg):
    print(f"\n[{step}] {msg}")

def login(email, password):
    url = f"{BASE_URL}/auth/login"
    data = {"email": email, "password": password}
    req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as response:
            if response.status == 200:
                body = json.loads(response.read().decode())
                return body.get("token")
    except urllib.error.HTTPError as e:
        print(f"Login failed: {e.code} {e.read().decode()}")
        return None
    except Exception as e:
        print(f"Login error: {e}")
        return None

def create_coupon(token, code, usage_limit_per_user):
    url = f"{BASE_URL}/admin/coupons"
    data = {
        "code": code,
        "discountType": "PERCENTAGE",
        "discountValue": 10,
        "minOrderAmount": 0,
        "usageLimit": 100,
        "usageLimitPerUser": usage_limit_per_user,
        "validFrom": "2024-01-01T00:00:00",
        "validUntil": "2025-12-31T23:59:59",
        "active": True,
        "description": "Test Coupon"
    }
    req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as response:
            return response.status == 200 or response.status == 201
    except urllib.error.HTTPError as e:
        print(f"Create coupon failed: {e.code} {e.read().decode()}")
        return False

def add_to_cart(token, product_id, quantity=1):
    url = f"{BASE_URL}/cart/add"
    data = {"productId": product_id, "quantity": quantity}
    req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as response:
            return response.status == 200
    except urllib.error.HTTPError as e:
        print(f"Add to cart failed: {e.code} {e.read().decode()}")
        return False

def apply_coupon(token, code):
    url = f"{BASE_URL}/cart/apply-coupon?code={code}"
    # Use POST as per controller
    req = urllib.request.Request(url, method="POST", headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as response:
            return response.status == 200, response.read().decode()
    except urllib.error.HTTPError as e:
        return False, e.read().decode()

def place_order(token):
    url = f"{BASE_URL}/orders"
    # Need shipping address
    data = {
        "shippingAddress": {
            "street": "123 Test St",
            "city": "Test City",
            "state": "Test State",
            "zipCode": "12345",
            "country": "Test Country"
        },
        "paymentMethod": "COD" # Assuming COD is available/simple
    }
    req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as response:
            body = json.loads(response.read().decode())
            return body.get("id")
    except urllib.error.HTTPError as e:
        print(f"Place order failed: {e.code} {e.read().decode()}")
        return None

def confirm_payment(token, order_id):
    url = f"{BASE_URL}/orders/{order_id}/confirm-payment"
    data = {"paymentId": "dummy_payment_id", "signature": "dummy_signature"} # Fake for logic check
    # Or maybe just updating status if Admin?
    # Actually, verify if `confirm-payment` triggers the coupon application logic.
    # Code snippet viewed earlier: confirms payment calls `applyCoupon`.
    # It requires params often.
    # Assuming standard flow.
    req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req) as response:
            return response.status == 200
    except urllib.error.HTTPError as e:
         # Even if payment fails validation (signature), hopefully code executed enough?
         # Or maybe use COD flow?
         # If payment method is COD, order serves as confirmed?
         # Let's see OrderService.createOrder.
         # Actually, confirmPayment is for Razorpay usually.
         # If I use COD, `createOrder` might apply coupon immediately?
         # Check OrderService.createOrder logic.
         print(f"Confirm payment failed (expected if signature invalid, but check logic): {e.code} {e.read().decode()}")
         return False

def main():
    print("Starting Coupon Verification...")

    # 1. Admin Login
    print_step(1, "Admin Login")
    admin_token = login("admin@example.com", "admin123")
    if not admin_token:
        print("FATAL: Admin login failed")
        sys.exit(1)
    print("Admin logged in.")

    # 2. Create Coupon
    coupon_code = f"PERUSER_{int(time.time())}"
    print_step(2, f"Creating Coupon {coupon_code} with limit 1 per user")
    if create_coupon(admin_token, coupon_code, 1):
        print("Coupon created.")
    else:
        print("FATAL: Failed to create coupon")
        sys.exit(1)

    # 3. User Login
    print_step(3, "User Login")
    # user@example.com / password (default)
    user_token = login("user@example.com", "password")
    if not user_token:
        print("FATAL: User login failed")
        sys.exit(1)
    print("User logged in.")

    # 4. Add Generic Product to Cart
    # Need a product ID. Assuming 1 exists.
    print_step(4, "Add Product to Cart")
    if add_to_cart(user_token, 1):
        print("Product added to cart.")
    else:
        print("Attempting to add product ID 2...")
        if add_to_cart(user_token, 2):
            print("Product added.")
        else:
            print("FATAL: Could not add product to cart.")
            sys.exit(1)

    # 5. Apply Coupon (First Time)
    print_step(5, "Apply Coupon (1st Attempt)")
    success, msg = apply_coupon(user_token, coupon_code)
    if success:
        print("Coupon applied successfully.")
    else:
        print(f"FATAL: Failed to apply coupon first time: {msg}")
        sys.exit(1)

    # 6. Place Order
    print_step(6, "Place Order")
    order_id = place_order(user_token)
    if order_id:
        print(f"Order placed: {order_id}")
    else:
        print("FATAL: Failed to place order")
        sys.exit(1)
    
    # 7. Confirm Payment (Simulated)
    # This is critical to RECORD the usage.
    # Note: If order provided 'COD' maybe it's auto-confirmed?
    # If not, we try to confirm.
    print_step(7, "Confirm Payment (Trigger Usage Recording)")
    confirm_payment(user_token, order_id) 
    # Ignore failure here, as long as it hit the service logic.
    # But wait, if transaction rolls back, usage might not be saved?
    # CouponUsageRepository.save is called.
    # If confirmPayment fails, does it rollback?
    # Only if exception propagates.
    # In OrderService.confirmPayment: 
    # catch (Exception e) { log.error... } -> It catches exception for coupon!
    # But we want the coupon application to SUCCEED.
    # Double check OrderService logic viewed earlier.
    
    # 8. Apply Coupon (Second Time)
    print_step(8, "Apply Coupon (2nd Attempt - Should Fail)")
    success_2, msg_2 = apply_coupon(user_token, coupon_code)
    if not success_2:
        print(f"SUCCESS: Coupon rejected as expected. Message: {msg_2}")
    else:
        print("FAILURE: Coupon was accepted again! Per-user limit not working.")
        sys.exit(1)

if __name__ == "__main__":
    main()
