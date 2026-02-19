$baseUrl = "http://localhost:8080/api"

function Get-Token {
    param ($email, $password)
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    
    for ($i = 0; $i -lt 5; $i++) {
        try {
            $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
            return $response.token
        }
        catch {
            Write-Host "Login attempt $($i+1) failed: $_"
            if ($_.Exception.Response) {
                Write-Host "Status: $($_.Exception.Response.StatusCode)"
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    Write-Host "Body: $($reader.ReadToEnd())"
                }
            }
            Start-Sleep -Seconds 2
        }
    }
    return $null
}

Write-Host "[1/8] Admin Login..."
$adminToken = Get-Token "admin@perfumeshop.local" "admin123456"
if (!$adminToken) { exit 1 }
Write-Host "Admin Token obtained."

$couponCode = "PERUSER_$(Get-Date -Format 'yyyyMMddHHmmss')"
Write-Host "[2/8] Creating Coupon $couponCode..."
$couponBody = @{
    code              = $couponCode
    discountType      = "PERCENTAGE"
    discountValue     = 10
    minOrderAmount    = 0
    usageLimit        = 100
    usageLimitPerUser = 1
    validFrom         = "2024-01-01T00:00:00"
    validUntil        = "2025-12-31T23:59:59"
    active            = $true
    description       = "Test Coupon"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/admin/coupons" -Method Post -Body $couponBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $adminToken" }
    Write-Host "Coupon Created."
}
catch {
    $statusCode = $_.Exception.Response.StatusCode
    Write-Error "Create Coupon Failed: $_ (Status: $statusCode)"
    exit 1
}

Write-Host "[3/8] User Login..."
$userToken = Get-Token "test@example.com" "password1"
if (!$userToken) { exit 1 }
Write-Host "User Token obtained."

Write-Host "[4/8] Add to Cart..."
$cartBody = @{ productId = 1; quantity = 1 } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "$baseUrl/cart/add" -Method Post -Body $cartBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $userToken" }
    Write-Host "Added to Cart."
}
catch {
    # Try product 2 if 1 fails
    Write-Host "Retrying with product 2..."
    $cartBody = @{ productId = 2; quantity = 1 } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/cart/add" -Method Post -Body $cartBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $userToken" }
        Write-Host "Added to Cart."
    }
    catch {
        Write-Error "Add to Cart Failed: $_"
        exit 1
    }
}

Write-Host "[5/8] Apply Coupon (1st Time)..."
try {
    Invoke-RestMethod -Uri "$baseUrl/cart/apply-coupon?code=$couponCode" -Method Post -Headers @{ Authorization = "Bearer $userToken" }
    Write-Host "Coupon Applied."
}
catch {
    Write-Error "Apply Coupon Failed: $_"
    exit 1
}

Write-Host "[6/8] Place Order..."
$orderBody = @{
    shippingAddress = @{
        street = "123 St"; city = "City"; state = "State"; zipCode = "12345"; country = "Country"
    }
    paymentMethod   = "COD"
} | ConvertTo-Json

try {
    $order = Invoke-RestMethod -Uri "$baseUrl/orders" -Method Post -Body $orderBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $userToken" }
    $orderId = $order.id
    Write-Host "Order Placed: $orderId"
}
catch {
    Write-Error "Place Order Failed: $_"
    exit 1
}

Write-Host "[7/8] Confirm Payment..."
$paymentBody = @{ paymentId = "dummy"; signature = "dummy" } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "$baseUrl/orders/$orderId/confirm-payment" -Method Post -Body $paymentBody -ContentType "application/json" -Headers @{ Authorization = "Bearer $userToken" }
    Write-Host "Payment Confirmed."
}
catch {
    Write-Host "Payment Confirmation returned error (expected for dummy signature): $_"
}

Write-Host "[8/8] Apply Coupon (2nd Time)..."
try {
    Invoke-RestMethod -Uri "$baseUrl/cart/apply-coupon?code=$couponCode" -Method Post -Headers @{ Authorization = "Bearer $userToken" }
    Write-Error "FAILURE: Coupon was accepted again!"
    exit 1
}
catch {
    $statusCode = $_.Exception.Response.StatusCode
    if ($statusCode -eq "BadRequest") {
        Write-Host "SUCCESS: Coupon rejected as expected."
    }
    else {
        Write-Error "Unexpected error: $_"
        exit 1
    }
}
