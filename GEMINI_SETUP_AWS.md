# 🤖 Google Gemini API Setup for Perfume Shop Chatbot

## Overview
Using Google Gemini API for FREE, production-ready AI chatbot on AWS.

**Why Gemini?**
✅ Free tier (60 requests/minute)
✅ Production-ready
✅ Works great with Spring AI
✅ No credit card needed for free tier
✅ Perfect for AWS deployment
✅ Business-grade reliability

---

## Step 1: Get Gemini API Key

### Option A: Using Free Tier (Recommended)
1. Go to [Google AI Studio](https://aistudio.google.com)
2. Click **Get API Key**
3. Create new API key in existing or new project
4. **Copy the API key** (looks like: `AIza...`)

### Option B: Using Google Cloud Console
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project
3. Enable **Google Generative AI API**
4. Create API key in Credentials
5. Copy the key

---

## Step 2: Configure in Your Project

### Development (Local)
Create/Update `.env` in project root:
```bash
GOOGLE_AI_API_KEY=AIza_your_actual_api_key_here
GOOGLE_PROJECT_ID=your-project-id
```

### Or set as System Environment Variables:
```bash
# Windows PowerShell
$env:GOOGLE_AI_API_KEY="AIza_your_api_key"
$env:GOOGLE_PROJECT_ID="your-project-id"

# Linux/Mac
export GOOGLE_AI_API_KEY="AIza_your_api_key"
export GOOGLE_PROJECT_ID="your-project-id"
```

### Production (AWS)
1. Go to AWS Systems Manager → Parameter Store
2. Create new parameters:
   - Name: `/perfume-shop/gemini-api-key`
   - Value: `AIza_your_key`
   - Type: `SecureString`
3. Create IAM role with access to Parameter Store
4. In Spring Boot application, add:
```yaml
spring:
  ai:
    google:
      generativeai:
        api-key: ${GOOGLE_AI_API_KEY}
        project-id: ${GOOGLE_PROJECT_ID}
```

---

## Step 3: Update Dependencies

### Maven (pom.xml)
Already updated to:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-google-generativeai-spring-boot-starter</artifactId>
    <version>0.8.1</version>
</dependency>
```

### Build
```bash
mvn clean install
```

---

## Step 4: Update Chatbot Service (If Needed)

The `ChatbotService.java` works with Gemini out of the box! No changes needed.

Gemini uses the same `ChatClient` interface as OpenAI in Spring AI.

---

## Step 5: Deploy on AWS

### Option A: Using AWS EC2
```bash
# SSH into EC2 instance
ssh -i your-key.pem ec2-user@your-instance.com

# Set environment variables
export GOOGLE_AI_API_KEY=AIza_your_key
export GOOGLE_PROJECT_ID=your-project-id

# Run the application
java -jar perfume-shop.jar
```

### Option B: Using AWS Elastic Beanstalk
Create `.ebextensions/01_env.config`:
```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    GOOGLE_AI_API_KEY: AIza_your_key
    GOOGLE_PROJECT_ID: your-project-id
```

### Option C: Using Docker on AWS ECS
Update `Dockerfile`:
```dockerfile
FROM openjdk:17-slim
COPY target/perfume-shop.jar app.jar
ENV GOOGLE_AI_API_KEY=${GOOGLE_AI_API_KEY}
ENV GOOGLE_PROJECT_ID=${GOOGLE_PROJECT_ID}
ENTRYPOINT ["java","-jar","app.jar"]
```

Update `docker-compose.yml`:
```yaml
services:
  backend:
    build: .
    environment:
      GOOGLE_AI_API_KEY: ${GOOGLE_AI_API_KEY}
      GOOGLE_PROJECT_ID: ${GOOGLE_PROJECT_ID}
    ports:
      - "8080:8080"
```

---

## Available Models

Gemini offers multiple models:

| Model | Speed | Quality | Cost |
|-------|-------|---------|------|
| **gemini-pro** | Very Fast | Good | FREE ✅ |
| **gemini-pro-vision** | Fast | Good+Vision | FREE ✅ |
| **gemini-1.5-pro** | Medium | Excellent | Paid |

**Default:** `gemini-pro` (best for free tier)

---

## API Limits (Free Tier)

- **Requests**: 60 per minute
- **Tokens**: 32K per request (input), 8K output
- **Uptime**: 99.5% SLA
- **Support**: Community

For your perfume shop chatbot, this is **more than enough!**

---

## Testing the Setup

### Test Endpoint
```bash
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I like fresh floral fragrances",
    "conversationHistory": ""
  }'
```

### Expected Response
```json
{
  "message": "Based on your preference for fresh florals...",
  "status": "success",
  "timestamp": 1674532400000
}
```

---

## Troubleshooting

### Issue: "API Key not found"
**Solution:**
1. Verify environment variable is set: `echo $GOOGLE_AI_API_KEY`
2. Check application.yml for correct property names
3. Restart application after setting env vars

### Issue: "Invalid API Key"
**Solution:**
1. Get new API key from Google AI Studio
2. Replace in environment variable
3. Ensure no extra spaces/quotes

### Issue: "Rate limit exceeded"
**Solution:**
1. Implement caching for common questions
2. Upgrade to paid tier if needed
3. Add request throttling in backend

### Issue: "Project ID not set"
**Solution:**
1. Optional for free tier, but recommended
2. Get from Google Cloud Console
3. Set GOOGLE_PROJECT_ID environment variable

---

## Cost Comparison

| Provider | Cost | Free Tier | AWS Compatible |
|----------|------|-----------|-----------------|
| **Gemini (Google)** | FREE/$) | 60 req/min ✅ | Yes ✅ |
| OpenAI | $0.0005/1K tokens | No | Yes |
| Claude | Similar to OpenAI | No | Yes |

**Savings:** $1-5/month vs $0 with Gemini!

---

## Next Steps

1. ✅ Get Gemini API key
2. ✅ Set environment variables
3. ✅ Update pom.xml (done)
4. ✅ Update application.yml (done)
5. ⏭️ **Rebuild**: `mvn clean package`
6. ⏭️ **Test**: Run application and test chatbot
7. ⏭️ **Deploy to AWS**: Follow deployment guide above

---

## Security Best Practices for AWS

1. **Never commit API keys to git**
   ```bash
   # Add to .gitignore
   .env
   *.local
   ```

2. **Use AWS Secrets Manager**
   ```java
   // In Spring Boot, use AWS Secrets Manager integration
   ```

3. **Rotate keys regularly**
   - Change API key every 3 months
   - Monitor usage in Google AI Studio

4. **Enable API quotas**
   - Go to Google Cloud Console
   - Set rate limits to prevent abuse

---

## Support

- **Google AI Studio**: https://aistudio.google.com
- **Google Generative AI Docs**: https://ai.google.dev
- **Spring AI Docs**: https://docs.spring.io/spring-ai/reference/

---

## Summary

✨ **Your Gemini chatbot is ready for AWS deployment!**

**Key Benefits:**
- 🎉 Completely FREE
- ⚡ Production-ready
- 🔒 Secure with AWS
- 📈 Scalable
- 💼 Business-grade

Get your API key and you're good to go! 🚀
