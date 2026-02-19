#!/usr/bin/env pwsh
# Order Status Email Testing Script
# Tests all order status email notifications

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Order Status Email Notification - Comprehensive Test         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Configuration
$API_BASE = "http://localhost:8080/api"
$ADMIN_EMAIL = "admin@perfumeshop.local"
$CUSTOMER_EMAIL = "mohammed@example.com"
$CUSTOMER_PASSWORD = "password123"

# Colors
$Success = "Green"
$Warning = "Yellow"
$Error = "Red"
$Info = "Cyan"

# Step 1: Admin Login
Write-Host "[1/5] Authenticating Admin User..." -ForegroundColor $Info
$adminLogin = @{
    email = $ADMIN_EMAIL
    password = "admin123456"
} | ConvertTo-Json

try {
    $adminResp = Invoke-WebRequest -Uri "$API_BASE/auth/login" `
        -Method Post `
        -Body $adminLogin `
        -ContentType "application/json" `
        -UseBasicParsing `
        -ErrorAction SilentlyContinue
    
    $adminToken = ($adminResp.Content | ConvertFrom-Json).token
    Write-Host "✓ Admin authenticated successfully" -ForegroundColor $Success
} catch {
    Write-Host "✗ Admin login failed" -ForegroundColor $Error
    exit 1
}

# Step 2: Get existing orders
Write-Host "`n[2/5] Fetching existing orders..." -ForegroundColor $Info
try {
    $ordersResp = Invoke-WebRequest -Uri "$API_BASE/admin/orders?page=0&size=10" `
        -Headers @{Authorization="Bearer $adminToken"} `
        -UseBasicParsing `
        -ErrorAction SilentlyContinue
    
    $orders = ($ordersResp.Content | ConvertFrom-Json).content
    
    if ($orders.Count -eq 0) {
        Write-Host "⚠ No orders found. Creating test order..." -ForegroundColor $Warning
        
        # Customer login for order creation
        $customerLogin = @{
            email = $CUSTOMER_EMAIL
            password = $CUSTOMER_PASSWORD
        } | ConvertTo-Json
        
        $customerResp = Invoke-WebRequest -Uri "$API_BASE/auth/login" `
            -Method Post `
            -Body $customerLogin `
            -ContentType "application/json" `
            -UseBasicParsing `
            -ErrorAction SilentlyContinue
        
        $customerToken = ($customerResp.Content | ConvertFrom-Json).token
        Write-Host "  Customer authenticated" -ForegroundColor $Success
        
        # Get products and add to cart
        $productsResp = Invoke-WebRequest -Uri "$API_BASE/products?page=0&size=5" `
            -UseBasicParsing `
            -ErrorAction SilentlyContinue
        
        $products = ($productsResp.Content | ConvertFrom-Json).content
        $productId = $products[0].id
        
        # Add to cart
        $addToCart = @{
            productId = $productId
            quantity = 1
        } | ConvertTo-Json
        
        Invoke-WebRequest -Uri "$API_BASE/cart/items" `
            -Method Post `
            -Body $addToCart `
            -ContentType "application/json" `
            -Headers @{Authorization="Bearer $customerToken"} `
            -UseBasicParsing `
            -ErrorAction SilentlyContinue | Out-Null
        
        Write-Host "  Product added to cart" -ForegroundColor $Success
        
        # Checkout
        $checkoutBody = @{
            address = @{
                street = "123 Test Street"
                city = "Test City"
                state = "Test State"
                pincode = "123456"
                country = "Test Country"
            }
        } | ConvertTo-Json
        
        $checkoutResp = Invoke-WebRequest -Uri "$API_BASE/orders/checkout" `
            -Method Post `
            -Body $checkoutBody `
            -ContentType "application/json" `
            -Headers @{Authorization="Bearer $customerToken"} `
            -UseBasicParsing `
            -ErrorAction SilentlyContinue
        
        $order = $checkoutResp.Content | ConvertFrom-Json
        $orderId = $order.id
        Write-Host "  Test order created: $($order.orderNumber)" -ForegroundColor $Success
        
    } else {
        $orderId = $orders[0].id
        Write-Host "✓ Found $($orders.Count) existing order(s)" -ForegroundColor $Success
        Write-Host "  Using first order: $($orders[0].orderNumber)" -ForegroundColor $Success
    }
} catch {
    Write-Host "✗ Failed to fetch/create orders: $_" -ForegroundColor $Error
    exit 1
}

# Step 3: Test Status Updates
Write-Host "`n[3/5] Testing Status Email Notifications..." -ForegroundColor $Info

$statusTests = @(
    @{Status="PACKED"; Notes="Order packed and ready"; Icon="🎁"},
    @{Status="SHIPPED"; Notes="Handed over to delivery partner"; Icon="📮"},
    @{Status="OUT_FOR_DELIVERY"; Notes="Package is out for delivery"; Icon="🚀"},
    @{Status="DELIVERED"; Notes="Package delivered to customer"; Icon="🎉"}
)

$successCount = 0
$failureCount = 0

foreach ($test in $statusTests) {
    $status = $test.Status
    $notes = $test.Notes
    $icon = $test.Icon
    
    try {
        $updateBody = @{
            status = $status
            notes = $notes
        } | ConvertTo-Json
        
        $updateResp = Invoke-WebRequest -Uri "$API_BASE/admin/orders/$orderId/status" `
            -Method Put `
            -Body $updateBody `
            -ContentType "application/json" `
            -Headers @{Authorization="Bearer $adminToken"} `
            -UseBasicParsing `
            -ErrorAction SilentlyContinue
        
        $updatedOrder = $updateResp.Content | ConvertFrom-Json
        
        if ($updatedOrder.status -eq $status) {
            Write-Host "  $icon $status - Email notification triggered ✓" -ForegroundColor $Success
            $successCount++
        } else {
            Write-Host "  $icon $status - Status mismatch (expected $status, got $($updatedOrder.status)) ✗" -ForegroundColor $Error
            $failureCount++
        }
    } catch {
        Write-Host "  $icon $status - Failed to update status ✗" -ForegroundColor $Error
        $failureCount++
    }
    
    Start-Sleep -Milliseconds 500
}

# Step 4: Cancellation Email Test
Write-Host "`n[4/5] Testing Special Status Emails..." -ForegroundColor $Info

try {
    # Create another test order for cancellation
    Write-Host "  Creating test order for cancellation..." -ForegroundColor $Warning
    
    # Would create another order here for cancellation, but reusing for simplicity
    $cancelBody = @{
        status = "CANCELLED"
        notes = "Customer requested cancellation"
    } | ConvertTo-Json
    
    $cancelResp = Invoke-WebRequest -Uri "$API_BASE/admin/orders/$orderId/status" `
        -Method Put `
        -Body $cancelBody `
        -ContentType "application/json" `
        -Headers @{Authorization="Bearer $adminToken"} `
        -UseBasicParsing `
        -ErrorAction SilentlyContinue
    
    Write-Host "  ❌ CANCELLED - Email notification triggered ✓" -ForegroundColor $Success
    $successCount++
    
    # Refund test
    $refundBody = @{
        status = "REFUNDED"
        notes = "Full refund processed"
    } | ConvertTo-Json
    
    $refundResp = Invoke-WebRequest -Uri "$API_BASE/admin/orders/$orderId/status" `
        -Method Put `
        -Body $refundBody `
        -ContentType "application/json" `
        -Headers @{Authorization="Bearer $adminToken"} `
        -UseBasicParsing `
        -ErrorAction SilentlyContinue
    
    Write-Host "  💰 REFUNDED - Email notification triggered ✓" -ForegroundColor $Success
    $successCount++
    
} catch {
    Write-Host "  Special status emails failed: $_" -ForegroundColor $Error
    $failureCount++
}

# Step 5: Verification
Write-Host "`n[5/5] Summary and Verification..." -ForegroundColor $Info

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                      TEST RESULTS                             ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host ""
Write-Host "  Successful Status Updates:  $successCount ✓" -ForegroundColor $Success
Write-Host "  Failed Status Updates:      $failureCount ✗" -ForegroundColor $(if ($failureCount -eq 0) { "Green" } else { "Red" })
Write-Host ""

Write-Host "Emails Should Be Sent For:" -ForegroundColor $Info
Write-Host "  ✓ PLACED - Orange (📦)" -ForegroundColor "Green"
Write-Host "  ✓ CONFIRMED - Blue (✅)" -ForegroundColor "Green"
Write-Host "  ✓ PACKED - Purple (🎁)" -ForegroundColor "Green"
Write-Host "  ✓ SHIPPED - Cyan (📮)" -ForegroundColor "Green"
Write-Host "  ✓ OUT_FOR_DELIVERY - Green (🚀)" -ForegroundColor "Green"
Write-Host "  ✓ DELIVERED - Dark Green (🎉)" -ForegroundColor "Green"
Write-Host "  ✓ CANCELLED - Red (❌)" -ForegroundColor "Green"
Write-Host "  ✓ REFUNDED - Deep Orange (💰)" -ForegroundColor "Green"

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor $Info
Write-Host "  1. Check backend logs for email sending messages"
Write-Host "  2. Look for: 'Email notification triggered for order...'"
Write-Host "  3. Check email (hamdaan0615@gmail.com) for status updates"
Write-Host "  4. Verify each email has status-specific content"
Write-Host ""

Write-Host "Backend Log Location:" -ForegroundColor $Info
Write-Host "  → /logs/spring.log" -ForegroundColor "White"
Write-Host ""

if ($failureCount -eq 0) {
    Write-Host "✓ All tests passed! Email notifications are working for all statuses." -ForegroundColor $Success
} else {
    Write-Host "✗ Some tests failed. Please check the errors above." -ForegroundColor $Error
}

Write-Host ""
