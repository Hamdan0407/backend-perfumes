# Chatbot AI Integration - Quick Start

## What Was Fixed

✅ **Dynamic AI Responses**: Chatbot now uses Google Gemini AI for intelligent, context-aware conversations
✅ **Hybrid System**: AI for general queries + Rule-based for instant product lookups
✅ **Product Knowledge**: AI knows your entire product catalog
✅ **Automatic Fallback**: If AI fails, seamlessly falls back to rule-based responses
✅ **Conversation Memory**: Maintains context across the entire conversation

---

## Setup (2 Minutes)

### Step 1: Get Gemini API Key (FREE)

1. Visit: https://makersuite.google.com/app/apikey
2. Click "Create API Key"
3. Copy the key (starts with `AIza...`)

### Step 2: Set the API Key

**Windows PowerShell:**
```powershell
$env:GEMINI_API_KEY="AIza_your_key_here"
```

**Or add to `.env` file in project root:**
```
GEMINI_API_KEY=AIza_your_key_here
```

### Step 3: Run the Application

```bash
# Backend
mvn spring-boot:run

# Frontend (new terminal)
cd frontend
npm run dev
```

---

## Test It Out

Try these queries in the chatbot:

**AI-Powered Conversations:**
- "I need help finding a perfume"
- "I'm looking for something romantic and elegant"
- "What would you recommend for daily office wear?"

**Instant Product Lookups (Rule-Based):**
- "Price of Dior Sauvage"
- "Is Gucci Bloom in stock?"
- "Tell me about Tom Ford Black Orchid"

---

## How It Works

```
User Message
    ↓
Is it a direct product query? (price, stock, etc.)
    ↓ YES → Rule-Based Response (instant, accurate)
    ↓ NO
Is AI enabled?
    ↓ YES → Gemini AI Response (dynamic, conversational)
    ↓ FAIL
Rule-Based Fallback (structured conversation)
```

---

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
app:
  gemini:
    enabled: true                    # Enable/disable AI
    model: gemini-1.5-flash         # AI model
    api-key: ${GEMINI_API_KEY:}     # From environment
```

---

## Troubleshooting

**Chatbot still gives static responses:**
- Check if `GEMINI_API_KEY` is set: `echo $env:GEMINI_API_KEY`
- Look for "AI Enabled: true" in backend logs
- Verify API key at https://makersuite.google.com/app/apikey

**API errors:**
- Check internet connection
- Verify API quota (free tier: 60 requests/minute)
- Check backend logs for detailed errors

**Want to disable AI temporarily?**
```yaml
app:
  gemini:
    enabled: false  # Uses rule-based system only
```

---

## Next Steps

The chatbot now has dynamic AI responses! Other issues you mentioned:

2. ✅ AI integration - **FIXED**
3. Product database connection - Ready to fix next
4. Conversation flow - Ready to improve next  
5. UI/UX - Ready to enhance next

Let me know when you're ready to tackle the next issue!
