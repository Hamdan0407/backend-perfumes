# Register sample users
$users = @(
    @{ email = "owner@perfume.com"; password = "Owner$2024Perfume"; firstName = "Priya"; lastName = "Sharma" },
    @{ email = "manager@perfume.com"; password = "Manager$2024Shop"; firstName = "Rajesh"; lastName = "Kumar" },
    @{ email = "warehouse1@perfume.com"; password = "Warehouse$2024One"; firstName = "Amit"; lastName = "Singh" },
    @{ email = "warehouse2@perfume.com"; password = "Warehouse$2024Two"; firstName = "Vikram"; lastName = "Patel" },
    @{ email = "customer1@example.com"; password = "Customer$2024One"; firstName = "John"; lastName = "Doe" },
    @{ email = "customer2@example.com"; password = "Customer$2024Two"; firstName = "Jane"; lastName = "Smith" },
    @{ email = "customer3@example.com"; password = "Customer$2024Three"; firstName = "Mike"; lastName = "Johnson" }
)

foreach ($user in $users) {
    $json = ConvertTo-Json $user -Compress
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" -Method Post -ContentType "application/json" -Body $json -UseBasicParsing -ErrorAction SilentlyContinue
        if ($r) {
            Write-Host "✓ Registered: $($user.email)"
        }
    } catch {
        Write-Host "✗ Failed to register $($user.email): $($_.Exception.Response.StatusCode)"
    }
}

# Test login
Write-Host "`nTesting login..."
$loginJson = '{"email":"owner@perfume.com","password":"Owner$2024Perfume"}'
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginJson -UseBasicParsing
    $data = $r.Content | ConvertFrom-Json
    Write-Host "✓ Login successful!"
    Write-Host "Email: $($data.email)"
    Write-Host "Role: $($data.role)"
    Write-Host "Token: $($data.token.Substring(0, 50))..."
} catch {
    Write-Host "✗ Login failed: $($_.Exception.Response.StatusCode)"
}
