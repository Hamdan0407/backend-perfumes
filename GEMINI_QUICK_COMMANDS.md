# 🚀 Quick Command Reference - Gemini Chatbot

## Environment Setup

### Set Gemini API Key (PowerShell)
```powershell
$env:GOOGLE_AI_API_KEY="AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE"
$env:GOOGLE_PROJECT_ID="perfume-shop"
```

### Set Gemini API Key (Command Prompt)
```batch
set GOOGLE_AI_API_KEY=AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE
set GOOGLE_PROJECT_ID=perfume-shop
```

### Set Gemini API Key (Linux/Mac)
```bash
export GOOGLE_AI_API_KEY="AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE"
export GOOGLE_PROJECT_ID="perfume-shop"
```

---

## Build & Run Commands

### Build Backend (Maven)
```bash
cd c:\Users\Hamdaan\Documents\maam
mvn clean package -DskipTests
```

### Run Backend (Port 8080)
```bash
java -jar target/perfume-shop.jar
```

### Build & Run in One Command
```powershell
$env:GOOGLE_AI_API_KEY="AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE"; mvn clean package -DskipTests; java -jar target/perfume-shop.jar
```

### Run Frontend (Port 3000)
```bash
cd frontend
npm install  # Only if needed
npm run dev
```

### Run Using Script
```powershell
.\RUN_GEMINI_CHATBOT.ps1
```

---

## Test API Endpoints

### Test Chatbot (Basic)
```bash
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What fragrances do you recommend?"}'
```

### Test Chatbot (Full)
```bash
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message":"I like fresh floral fragrances",
    "conversationHistory":"User: What are you?",
    "userId":"user123",
    "sessionId":"session456"
  }'
```

### Test Recommendation
```bash
curl http://localhost:8080/api/chatbot/recommend?type=floral
```

### Test Preference Analysis
```bash
curl http://localhost:8080/api/chatbot/analyze-preference?preference=fresh
```

### Test Health
```bash
curl http://localhost:8080/api/chatbot/health
```

---

## Useful Diagnostics

### Check if Port is in Use
```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :3000
```

### Kill Process on Port
```powershell
# Find PID for port 8080
netstat -ano | findstr :8080

# Kill it
taskkill /PID <PID> /F

# Example: taskkill /PID 5432 /F
```

### Check Java Version
```bash
java -version
```

### Check Maven Version
```bash
mvn -version
```

### Check Node Version
```bash
node -v
npm -v
```

---

## Frontend

### Install Dependencies
```bash
cd frontend
npm install
```

### Run Development Server
```bash
npm run dev
```

### Build for Production
```bash
npm run build
```

### Preview Production Build
```bash
npm run preview
```

---

## Database Commands

### Connect to MySQL
```bash
mysql -u root -p
```

### Run SQL File
```bash
mysql -u root -p perfume_shop < init.sql
```

### Check MySQL Status
```bash
mysql -u root -p -e "SELECT 1;"
```

---

## Docker Commands (Optional)

### Build Docker Image
```bash
docker build -t perfume-shop:latest .
```

### Run Docker Container
```bash
docker run -p 8080:8080 \
  -e GOOGLE_AI_API_KEY=AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE \
  -e GOOGLE_PROJECT_ID=perfume-shop \
  perfume-shop:latest
```

### Docker Compose
```bash
docker-compose up --build
```

---

## Troubleshooting Commands

### Clear Maven Cache
```bash
mvn clean
rm -rf ~/.m2/repository/org/springframework/ai
```

### Clear Node Modules
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Check Logs
```bash
# Backend logs (if saved)
tail -f build.log

# View Maven output
mvn clean package -DskipTests -X  # Verbose
```

### Verify Configuration
```bash
# Check if environment variables are set
echo %GOOGLE_AI_API_KEY%  # Windows
echo $env:GOOGLE_AI_API_KEY  # PowerShell
echo $GOOGLE_AI_API_KEY  # Linux
```

---

## Windows-Specific Commands

### Find Port Usage (Windows)
```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

### Open Ports in Windows Firewall
```powershell
# Allow Java
New-NetFirewallRule -DisplayName "Java Port 8080" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080

# Allow Node
New-NetFirewallRule -DisplayName "Node Port 3000" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 3000
```

### Run as Administrator
```powershell
Start-Process powershell -Verb RunAs
```

---

## Production Deployment (AWS)

### Build JAR for Production
```bash
mvn clean package -Pproduction -DskipTests
```

### Deploy to EC2
```bash
scp -i your-key.pem target/perfume-shop.jar ec2-user@your-instance.com:/home/ec2-user/
ssh -i your-key.pem ec2-user@your-instance.com
cd /home/ec2-user/
GOOGLE_AI_API_KEY=your_key java -jar perfume-shop.jar
```

### Deploy to AWS Elastic Beanstalk
```bash
eb init
eb create perfume-shop-env
eb deploy
eb setenv GOOGLE_AI_API_KEY=AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE
```

---

## Useful Links

- **Google AI Studio**: https://aistudio.google.com
- **Google Generative AI Docs**: https://ai.google.dev
- **Spring AI Docs**: https://docs.spring.io/spring-ai/reference/
- **Maven Central**: https://mvnrepository.com
- **Java Downloads**: https://www.oracle.com/java/technologies/downloads/
- **Node.js**: https://nodejs.org/

---

## Quick Start (Copy-Paste)

### Full Setup in PowerShell
```powershell
# 1. Set environment variables
$env:GOOGLE_AI_API_KEY="AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE"
$env:GOOGLE_PROJECT_ID="perfume-shop"

# 2. Navigate to project
cd c:\Users\Hamdaan\Documents\maam

# 3. Build
mvn clean package -DskipTests

# 4. Run backend (in this terminal)
java -jar target/perfume-shop.jar &

# 5. Run frontend (in another terminal)
cd frontend
npm run dev

# 6. Open in browser
Start-Process "http://localhost:3000"
```

---

## API Key Info

**Your Gemini API Key**: `AIzaSyDoTdBrb9fPrmBGapbWLN6WRAgIPjLu6aE`

✅ Free tier (60 requests/minute)  
✅ No credit card required  
✅ Production-ready for MVP  
✅ Can be rotated anytime  

⚠️ Keep it private! Don't commit to GitHub.

---

**Last Updated**: January 25, 2026  
**Status**: ✅ OpenAI Removed, Gemini Configured, Ready to Deploy
