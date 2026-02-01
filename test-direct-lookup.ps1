# Test direct product lookup without conversation flow
$convId = "direct-test-$(Get-Random)"

Write-Host "Testing Direct Product Lookup Feature`n"
Write-Host "========================================`n"

# Test 1: Direct price query
Write-Host "TEST 1: Direct Price Query" -ForegroundColor Cyan
$body = @{conversationId = $convId; message = "Gucci Bloom price"} | ConvertTo-Json
$resp = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body -UseBasicParsing
$data = $resp.Content | ConvertFrom-Json
Write-Host "USER: Gucci Bloom price"
Write-Host "BOT: $($data.message)`n"

# Test 2: Direct stock query
Write-Host "TEST 2: Direct Stock Query" -ForegroundColor Cyan
$body2 = @{conversationId = "stock-test-$(Get-Random)"; message = "Is Dior Sauvage available?"} | ConvertTo-Json
$resp2 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body2 -UseBasicParsing
$data2 = $resp2.Content | ConvertFrom-Json
Write-Host "USER: Is Dior Sauvage available?"
Write-Host "BOT: $($data2.message)`n"

# Test 3: Direct product info query
Write-Host "TEST 3: Direct Product Info Query" -ForegroundColor Cyan
$body3 = @{conversationId = "info-test-$(Get-Random)"; message = "Tell me about Chanel No. 5"} | ConvertTo-Json
$resp3 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body3 -UseBasicParsing
$data3 = $resp3.Content | ConvertFrom-Json
Write-Host "USER: Tell me about Chanel No. 5"
Write-Host "BOT: $($data3.message)`n"

# Test 4: Simple product name query
Write-Host "TEST 4: Simple Product Name Query" -ForegroundColor Cyan
$body4 = @{conversationId = "name-test-$(Get-Random)"; message = "Creed Aventus"} | ConvertTo-Json
$resp4 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body4 -UseBasicParsing
$data4 = $resp4.Content | ConvertFrom-Json
Write-Host "USER: Creed Aventus"
Write-Host "BOT: $($data4.message)`n"

Write-Host "========================================" -ForegroundColor Green
Write-Host "All direct lookup tests complete!" -ForegroundColor Green
