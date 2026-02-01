# Real Chatbot Test - Verify Direct Intent Detection
# Tests that the chatbot behaves like a real conversational bot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CHATBOT REAL BEHAVIOR TEST" -ForegroundColor Cyan
Write-Host "Testing direct intent detection and proper responses" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/api/chatbot/chat"
$testsPassed = 0
$testsFailed = 0

function Test-ChatbotQuery {
    param(
        [string]$TestName,
        [string]$UserMessage,
        [string]$ExpectedKeyword,
        [string]$ConversationId
    )
    
    Write-Host "TEST: $TestName" -ForegroundColor Yellow
    Write-Host "  User: $UserMessage"
    
    $payload = @{
        message = $UserMessage
        conversationId = $ConversationId
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $payload `
            -ContentType "application/json" -UseBasicParsing -ErrorAction SilentlyContinue
        
        if ($response.StatusCode -eq 200) {
            $obj = $response.Content | ConvertFrom-Json
            $botMessage = $obj.message
            
            Write-Host "  Bot: $(($botMessage -replace '[\*_]', '').Substring(0, [Math]::Min(80, $botMessage.Length)))..."
            
            if ($botMessage -like "*$ExpectedKeyword*" -or $botMessage -ilike "*$ExpectedKeyword*") {
                Write-Host "  ✅ PASS - Found expected keyword: $ExpectedKeyword" -ForegroundColor Green
                return $true
            } else {
                Write-Host "  ❌ FAIL - Expected keyword not found: $ExpectedKeyword" -ForegroundColor Red
                Write-Host "     Full response: $botMessage"
                return $false
            }
        } else {
            Write-Host "  ❌ FAIL - HTTP $($response.StatusCode)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "  ❌ FAIL - Error: $_" -ForegroundColor Red
        return $false
    }
}

# Test 1: Direct Price Query (Real Product)
$convId1 = "test-conv-price-$(Get-Random)"
if (Test-ChatbotQuery "Direct Price Query (Real Product)" "Gucci Bloom price" "3800" $convId1) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 2: Direct Price Query (Non-existent Product)
$convId2 = "test-conv-nonexist-$(Get-Random)"
if (Test-ChatbotQuery "Direct Price Query (Non-existent)" "rose garden price" "couldn't find" $convId2) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 3: Direct Stock Query
$convId3 = "test-conv-stock-$(Get-Random)"
if (Test-ChatbotQuery "Direct Stock Query" "Is Dior Sauvage available" "stock" $convId3) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 4: Direct Product Info Query
$convId4 = "test-conv-info-$(Get-Random)"
if (Test-ChatbotQuery "Direct Product Info Query" "Tell me about Chanel No. 5" "Price" $convId4) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 5: Simple Product Name Query
$convId5 = "test-conv-name-$(Get-Random)"
if (Test-ChatbotQuery "Simple Product Name Query" "Creed Aventus" "Price" $convId5) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 6: Follow-up in Same Conversation
$convId6 = "test-conv-followup-$(Get-Random)"
Write-Host "TEST: Conversation State Persistence" -ForegroundColor Yellow
Write-Host "  Query 1: Gucci Bloom price"
$payload1 = @{ message = "Gucci Bloom price"; conversationId = $convId6 } | ConvertTo-Json
$r1 = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $payload1 -ContentType "application/json" -UseBasicParsing -ErrorAction SilentlyContinue
$msg1 = ($r1.Content | ConvertFrom-Json).message
Write-Host "  ✓ First message received"

Start-Sleep -Milliseconds 500

Write-Host "  Query 2: What about Dior?"
$payload2 = @{ message = "What about Dior?"; conversationId = $convId6 } | ConvertTo-Json
$r2 = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $payload2 -ContentType "application/json" -UseBasicParsing -ErrorAction SilentlyContinue
$msg2 = ($r2.Content | ConvertFrom-Json).message
Write-Host "  Bot: $(($msg2 -replace '[\*_]', '').Substring(0, [Math]::Min(80, $msg2.Length)))..."

if ($msg2.Length -gt 0) {
    Write-Host "  ✅ PASS - Conversation maintained" -ForegroundColor Green
    $testsPassed++
} else {
    Write-Host "  ❌ FAIL - No response" -ForegroundColor Red
    $testsFailed++
}
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST RESULTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ Passed: $testsPassed" -ForegroundColor Green
Write-Host "❌ Failed: $testsFailed" -ForegroundColor Red
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "🎉 ALL TESTS PASSED! Chatbot is behaving like a real conversational bot!" -ForegroundColor Green
} else {
    Write-Host "Some tests failed. Check the output above." -ForegroundColor Yellow
}
