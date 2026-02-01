# 🤖 Chatbot Integration - Complete Setup Guide

## Overview

A **Smart Scent Consultation Chatbot** has been integrated into the Perfume Shop using Spring AI and OpenAI's GPT models.

The chatbot provides:
- ✨ Personalized fragrance recommendations
- 🎯 Scent preference analysis  
- 💬 Customer support and FAQs
- 🌸 Interactive consultation widget

---

## Architecture

### Backend (Spring Boot)
- **Service**: `ChatbotService.java`
- **Controller**: `ChatbotController.java`
- **DTOs**: `ChatRequest.java`, `ChatResponse.java`
- **AI Framework**: Spring AI with OpenAI integration

### Frontend (React)
- **Component**: `Chatbot.jsx`
- **Location**: `/components/Chatbot.jsx`
- **Style**: Floating widget with gradient design
- **Features**: Real-time messaging, quick suggestions, conversation history

---

## Setup Instructions

### Step 1: Get OpenAI API Key

1. Go to [OpenAI Platform](https://platform.openai.com)
2. Sign up or login
3. Navigate to **API Keys** section
4. Click **Create new secret key**
5. Copy the key (looks like: `sk_test_...`)
6. **⚠️ Never share this key publicly**

### Step 2: Configure Environment Variables

#### Development (Local)
Create `.env` file in project root:
```bash
GOOGLE_AI_API_KEY=AIza_your_actual_api_key_here
GOOGLE_PROJECT_ID=your-project-id
```

#### Production
Set environment variable on server:
```bash
export GOOGLE_AI_API_KEY=AIza_your_production_api_key
export GOOGLE_PROJECT_ID=your-project-id
```

#### Docker
Add to `docker-compose.yml`:
```yaml
environment:
  GOOGLE_AI_API_KEY: ${GOOGLE_AI_API_KEY}
  GOOGLE_PROJECT_ID: ${GOOGLE_PROJECT_ID}
```

### Step 3: Build and Deploy

```bash
# Build backend
mvn clean package

# Frontend (already installed)
npm install
npm run dev

GOOGLE_AI_API_KEY=AIza_your_key GOOGLE_PROJECT_ID=your_project_idvariable
OPENAI_API_KEY=your_key java -jar target/perfume-shop.jar
```

---

## API Endpoints

### 1. Main Chat Endpoint
```
POST /api/chatbot/chat
```

**Request:**
```json
{
  "message": "I like fresh floral scents, what would you recommend?",
  "conversationHistory": "User: Hi\nBot: Welcome...",
  "userId": "user123",
  "sessionId": "session-xyz"
}
```

**Response:**
```json
{
  "message": "Based on your preference for fresh florals, I'd recommend...",
  "status": "success",
  "timestamp": 1674532400000,
  "recommendedProducts": "[...]",
  "confidence": 0.95
}
```

### 2. Quick Recommendation
```
GET /api/chatbot/recommend?type=floral
```

**Response:**
```json
{
  "message": "You might love our **Rose Garden** and **Jasmine Essence**...",
  "status": "success",
  "timestamp": 1674532400000
}
```

Types supported:
- `floral` - Flower-based fragrances
- `woody` - Wood and earthy scents
- `fresh` - Light and airy scents
- `oriental` - Warm and spicy scents
- `fruity` - Fruit-based fragrances

### 3. Analyze Preference
```
GET /api/chatbot/analyze-preference?preference=fresh&light
```

### 4. Health Check
```
GET /api/chatbot/health
```

---

## Frontend Implementation

### Basic Usage

```jsx
import Chatbot from './components/Chatbot';

function App() {
  return (
    <div>
      <YourContent />
      <Chatbot />
    </div>
  );
}
```

### Chatbot Features

1. **Floating Widget**
   - Appears as pink/purple button in bottom-right
   - Click to open/close conversation
   - Non-intrusive, always accessible

2. **Message Types**
   - User messages (right-aligned, pink background)
   - Bot messages (left-aligned, white background)
   - Timestamps for each message

3. **Quick Suggestions**
   - 4 fragrance type buttons (Floral, Woody, Fresh, Oriental)
   - One-click recommendations
   - Appear on initial chat

4. **Conversation History**
   - Maintains context across messages
   - Sent to backend for better recommendations
   - Improves response quality

---

## Customization

### Change Bot Personality

Edit `ChatbotService.java` - `buildSystemPrompt()` method:

```java
private String buildSystemPrompt() {
    return """
You are a professional Perfume Expert...
// Customize the instructions here
""";
}
```

### Add New Quick Suggestions

In `ChatbotService.java` - `quickRecommendation()` method:

```java
public String quickRecommendation(String fragmentType) {
    return switch (fragmentType.toLowerCase()) {
        case "custom_type" -> "Your custom response here...";
        // Add more cases
    };
}
```

### Modify UI/Styling

Edit `Chatbot.jsx`:
- Change colors: `bg-pink-600`, `bg-purple-600`
- Adjust size: `w-96`, `h-[600px]`
- Update emoji and icons
- Modify button positions

---

## Best Practices

### 1. API Key Security
✅ **DO:**
- Use environment variables
- Rotate keys periodically
- Use separate keys for dev/prod
- Monitor API usage

❌ **DON'T:**
- Hardcode API keys
- Commit keys to git
- Share keys in messages/emails
- Use same key for dev and production

### 2. Conversation Management
✅ **DO:**
- Send conversation history for context
- Limit history length to prevent token overuse
- Store conversations for user analysis
- Rate-limit API calls

❌ **DON'T:**
- Send entire history without filtering
- Allow unlimited API calls
- Store sensitive user data in conversation

### 3. Error Handling
✅ **DO:**
- Provide fallback responses
- Log errors for debugging
- Show user-friendly error messages
- Handle timeout gracefully

❌ **DON'T:**
- Expose API errors to users
- Ignore failed requests
- Leave user without response

---

## Troubleshooting

### Issue: "Invalid API Key"
**Solution:**
1. Check API key is correct
2. Verify it's not expired
3. Ensure environment variable is set
4. Restart application

### Issue: "Rate Limit Exceeded"
**Solution:**
1. Implement request throttling
2. Add queue system
3. Upgrade OpenAI plan
4. Cache frequent responses

### Issue: Chatbot not visible
**Solution:**
1. Check browser console (F12)
2. Verify Chatbot component imported in App.jsx
3. Clear browser cache
4. Check z-index conflicts

### Issue: Backend not responding
**Solution:**
1. Verify backend is running (port 8080)
2. Check CORS configuration
3. Verify API endpoint URL
4. Check network connectivity

---

## Cost Estimation

### OpenAI Pricing (As of 2024)
- **GPT-3.5-turbo**: ~$0.0005 per 1K tokens
- **GPT-4**: ~$0.03 per 1K tokens (10x more expensive)

### Example Costs
- Average message: ~100 tokens = $0.00005
- 1000 messages/day: ~$0.05/day = ~$1.50/month (GPT-3.5)
- 1000 messages/day with GPT-4: ~$1.50/day = ~$45/month

**Recommendation:** Start with GPT-3.5-turbo for cost efficiency. Upgrade to GPT-4 if better quality is needed.

---

## Enhancement Ideas

### Phase 1 (Current)
✅ Basic chat interface
✅ Scent recommendations
✅ Quick suggestions
✅ Conversation history

### Phase 2 (Planned)
🔜 User preferences persistence
🔜 Product purchase integration
🔜 Review sentiment analysis
🔜 Chatbot analytics dashboard

### Phase 3 (Future)
🔮 Multi-language support
🔮 Voice input/output
🔮 Image-based scent matching
🔮 ML-based recommendation engine

---

## Support & Resources

- **Google AI Studio**: https://aistudio.google.com
- **Google Generative AI Docs**: https://ai.google.dev
- **Spring AI Documentation**: https://docs.spring.io/spring-ai/reference/

---

## Summary

✨ **Your chatbot is now ready to enhance customer experience!**

Next steps:
1. Get Google Gemini API key (free from Google AI Studio)
2. Set environment variable
3. Rebuild and deploy
4. Test chatbot on website
5. Monitor usage and gather feedback

