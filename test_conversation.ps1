param(
    [string]$ConvId = "final-proof-rec"
)

Write-Host "=== TESTING RECOMMENDATIONS STORAGE AND RETRIEVAL ===" -ForegroundColor Cyan
Write-Host "ConversationId: $ConvId`n" -ForegroundColor Yellow

# Message 1: Greeting
Write-Host "[MSG 1] User: hello" -ForegroundColor Green
$body = @{
    message = "hello"
    conversationId = $ConvId
} | ConvertTo-Json

$r1 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
$msg1 = ($r1.Content | ConvertFrom-Json).message
Write-Host "Bot Response (first 100 chars): $($msg1.Substring(0, [Math]::Min(100, $msg1.Length)))`n" -ForegroundColor Cyan
Start-Sleep -Seconds 1

# Message 2: Romantic date occasion
Write-Host "[MSG 2] User: romantic date" -ForegroundColor Green
$body = @{
    message = "romantic date"
    conversationId = $ConvId
} | ConvertTo-Json

$r2 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
$msg2 = ($r2.Content | ConvertFrom-Json).message
Write-Host "Bot Response (first 100 chars): $($msg2.Substring(0, [Math]::Min(100, $msg2.Length)))`n" -ForegroundColor Cyan
Start-Sleep -Seconds 1

# Message 3: Floral scent
Write-Host "[MSG 3] User: floral" -ForegroundColor Green
$body = @{
    message = "floral"
    conversationId = $ConvId
} | ConvertTo-Json

$r3 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
$msg3 = ($r3.Content | ConvertFrom-Json).message
Write-Host "Bot Response (first 100 chars): $($msg3.Substring(0, [Math]::Min(100, $msg3.Length)))`n" -ForegroundColor Cyan
Start-Sleep -Seconds 1

# Message 4: Budget 3000-5000
Write-Host "[MSG 4] User: 3000 to 5000" -ForegroundColor Green
$body = @{
    message = "3000 to 5000"
    conversationId = $ConvId
} | ConvertTo-Json

$r4 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
$msg4 = ($r4.Content | ConvertFrom-Json).message
Write-Host "Bot Response:" -ForegroundColor Cyan
Write-Host $msg4 | Select-Object -First 15
Write-Host ""
Start-Sleep -Seconds 2

# MESSAGE 5: THE CRITICAL TEST - Ask for perfume names
Write-Host "[MSG 5] User: What are the perfume names?" -ForegroundColor Magenta
$body = @{
    message = "what are the perfume names?"
    conversationId = $ConvId
} | ConvertTo-Json

$r5 = Invoke-WebRequest -Uri "http://localhost:8080/api/chatbot/chat" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
$msg5 = ($r5.Content | ConvertFrom-Json).message

Write-Host "Bot Response:" -ForegroundColor Magenta
Write-Host $msg5 -ForegroundColor Yellow
Write-Host ""

# Now check backend logs
Write-Host "=== BACKEND LOGS FOR THIS CONVERSATION ===" -ForegroundColor Cyan
docker logs perfume-shop-api 2>&1 | Select-String $ConvId -Context 3 | Select-Object -Last 100
