# Final comprehensive test
$tests = @(
    @{name="Real Product Price"; msg="Gucci Bloom price"; expectKeywords=@("3800", "Gucci")},
    @{name="Non-existent Product Price"; msg="rose garden price"; expectKeywords=@("couldn't find", "catalog")},
    @{name="Real Product Stock"; msg="Is Dior Sauvage available?"; expectKeywords=@("in stock", "units")},
    @{name="Real Product Info"; msg="Tell me about Chanel No. 5"; expectKeywords=@("iconic", "classic")},
    @{name="Simple Product Name"; msg="Creed Aventus"; expectKeywords=@("8500", "Creed")}
)

Write-Host "========================================" -ForegroundColor Green
Write-Host "FINAL COMPREHENSIVE TEST" -ForegroundColor Green
Write-Host "========================================`n"

foreach ($test in $tests) {
    Write-Host "Test: $($test.name)" -ForegroundColor Cyan
    Write-Host "User: $($test.msg)"
    
    try {
        $body = @{conversationId="test-$(Get-Random)"; message=$test.msg} | ConvertTo-Json
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" `
            -Method POST `
            -Headers @{"Content-Type"="application/json"} `
            -Body $body `
            -UseBasicParsing `
            -TimeoutSec 10
        
        $data = $response.Content | ConvertFrom-Json
        $botMsg = $data.message
        
        Write-Host "Bot: $botMsg"
        
        # Check if expected keywords are present
        $found = $false
        foreach ($keyword in $test.expectKeywords) {
            if ($botMsg -like "*$keyword*") {
                $found = $true
                break
            }
        }
        
        if ($found) {
            Write-Host "Result: PASS" -ForegroundColor Green
        } else {
            Write-Host "Result: FAIL (expected keywords: $($test.expectKeywords -join ', '))" -ForegroundColor Red
        }
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
    
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "All tests completed!" -ForegroundColor Green
