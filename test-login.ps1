# Wait for backend
Start-Sleep 35

# Test login API
$body = @{
    email = 'admin@test.com'
    password = 'AdminP@ssw0rd'
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest 'http://localhost:8080/api/auth/login' `
        -Method POST `
        -Body $body `
        -ContentType 'application/json' `
        -UseBasicParsing `
        -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Response:"
    $response.Content | ConvertFrom-Json | ConvertTo-Json
} catch {
    Write-Host "Error Status: $($_.Exception.Response.StatusCode)"
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $reader.DiscardBufferedData()
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
