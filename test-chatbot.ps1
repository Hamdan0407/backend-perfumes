#!/usr/bin/env pwsh

$conversationId = "test-$(Get-Random)"
Write-Host "Testing Chatbot with conversation ID: $conversationId`n"

# Test 1: Greeting
Write-Host "=== TEST 1: Greeting ===" -ForegroundColor Cyan
$body1 = @{conversationId = $conversationId; message = "hello"} | ConvertTo-Json
$response1 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body1 `
    -UseBasicParsing
$data1 = $response1.Content | ConvertFrom-Json
Write-Host "USER: hello"
Write-Host "BOT: $($data1.response)`n"

# Test 2: Occasion
Write-Host "=== TEST 2: Occasion ===" -ForegroundColor Cyan
$body2 = @{conversationId = $conversationId; message = "I need it for a romantic date"} | ConvertTo-Json
$response2 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body2 `
    -UseBasicParsing
$data2 = $response2.Content | ConvertFrom-Json
Write-Host "USER: I need it for a romantic date"
Write-Host "BOT: $($data2.response)`n"

# Test 3: Scent preference
Write-Host "=== TEST 3: Scent Type ===" -ForegroundColor Cyan
$body3 = @{conversationId = $conversationId; message = "floral scents please"} | ConvertTo-Json
$response3 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body3 `
    -UseBasicParsing
$data3 = $response3.Content | ConvertFrom-Json
Write-Host "USER: floral scents please"
Write-Host "BOT: $($data3.response)`n"

# Test 4: Budget (should trigger recommendations)
Write-Host "=== TEST 4: Budget & Recommendations ===" -ForegroundColor Cyan
$body4 = @{conversationId = $conversationId; message = "my budget is 3000 to 5000"} | ConvertTo-Json
$response4 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body4 `
    -UseBasicParsing
$data4 = $response4.Content | ConvertFrom-Json
Write-Host "USER: my budget is 3000 to 5000"
Write-Host "BOT: $($data4.response)`n"

# Test 5: CRITICAL - Ask for product details
Write-Host "=== TEST 5: CRITICAL - Product Price Lookup ===" -ForegroundColor Green
$body5 = @{conversationId = $conversationId; message = "what is the price of Gucci Bloom?"} | ConvertTo-Json
$response5 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" `
    -Method POST `
    -Headers @{"Content-Type" = "application/json"} `
    -Body $body5 `
    -UseBasicParsing
$data5 = $response5.Content | ConvertFrom-Json
Write-Host "USER: what is the price of Gucci Bloom?"
Write-Host "BOT: $($data5.response)`n"

Write-Host "✓ Conversation test complete" -ForegroundColor Green
