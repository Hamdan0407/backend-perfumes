# Real Chatbot Test - Verify Direct Intent Detection
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
            
            $display = $botMessage -replace '[\*_]', ''
            $display = $display.Substring(0, [Math]::Min(80, $display.Length))
            Write-Host "  Bot: $display..."
            
            if ($botMessage -like "*$ExpectedKeyword*" -or $botMessage -ilike "*$ExpectedKeyword*") {
                Write-Host "  OK PASS - Found expected keyword: $ExpectedKeyword" -ForegroundColor Green
                return $true
            } else {
                Write-Host "  FAIL - Expected keyword not found: $ExpectedKeyword" -ForegroundColor Red
                return $false
            }
        } else {
            Write-Host "  FAIL - HTTP $($response.StatusCode)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "  FAIL - Error: $_" -ForegroundColor Red
        return $false
    }
}

# Test 1: Direct Price Query (Real Product)
$convId1 = "test-conv-price-$(Get-Random)"
if (Test-ChatbotQuery "Direct Price Query" "Gucci Bloom price" "3800" $convId1) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 2: Direct Stock Query
$convId3 = "test-conv-stock-$(Get-Random)"
if (Test-ChatbotQuery "Direct Stock Query" "Is Dior Sauvage available" "stock" $convId3) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 3: Direct Product Info Query
$convId4 = "test-conv-info-$(Get-Random)"
if (Test-ChatbotQuery "Direct Product Info Query" "Tell me about Chanel No. 5" "Price" $convId4) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Test 4: Simple Product Name Query
$convId5 = "test-conv-name-$(Get-Random)"
if (Test-ChatbotQuery "Simple Product Name Query" "Creed Aventus" "Price" $convId5) {
    $testsPassed++
} else {
    $testsFailed++
}
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST RESULTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor Red
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "SUCCESS! Chatbot working as real conversational bot!" -ForegroundColor Green
}
