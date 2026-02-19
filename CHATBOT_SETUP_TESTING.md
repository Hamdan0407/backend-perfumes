# Intelligent Chatbot - Setup & Testing Guide

## Quick Start

### 1. Build Backend
```bash
cd c:\Users\Hamdaan\OneDrive\Documents\maam
mvn clean package -DskipTests -q
```

### 2. Run Backend
```bash
java -jar target/perfume-shop-1.0.0.jar --server.port=8080
```

### 3. Build & Run Frontend
```bash
cd frontend
npm run build
npm run preview
# Or for development:
npm start
```

The frontend will be available at: `http://localhost:5173` (Vite preview) or `http://localhost:3000` (dev server)

---

## Testing the Intelligent Chatbot

### Using the Web Interface
1. Open `http://localhost:5173`
2. Click the pink chat button (bottom-right corner)
3. Start asking questions!

### Example Test Queries

#### 1. **Price Query**
```
User: "Gucci Bloom price"
Expected: Returns exact price from database (₹3800), stock status, rating
```

#### 2. **Availability Check**
```
User: "Is Dior Sauvage in stock?"
Expected: Shows availability with quantity
```

#### 3. **Budget-based Recommendation**
```
User: "Perfumes under 3000"
Expected: Lists products in price range with ratings
```

#### 4. **Occasion-based Recommendation**
```
User: "What perfume for work?"
Expected: Recommends professional fragrances
```

#### 5. **Product Comparison**
```
User: "Compare Gucci Bloom vs Dior Sauvage"
Expected: Side-by-side comparison table
```

#### 6. **Scent Type Query**
```
User: "Tell me about floral fragrances"
Expected: Floral fragrance information and recommendations
```

#### 7. **Trending Products**
```
User: "What are trending products?"
Expected: Popular products with high ratings and reviews
```

### Using cURL (Backend Testing)

#### Price Query
```bash
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the price of Gucci Bloom?",
    "conversationId": "test-conv-1"
  }' | jq .
```

#### Recommendation Query
```bash
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I need a fragrance for work, something fresh and professional",
    "conversationId": "test-conv-2"
  }' | jq .
```

#### Budget Query
```bash
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Show me perfumes under 5000 rupees",
    "conversationId": "test-conv-3"
  }' | jq .
```

---

## Key Features to Test

### ✅ Intent Detection
- The bot should automatically identify what you're asking about
- Check console logs for "Detected user intent:" message

### ✅ Smart Recommendations
- Recommendations should match your stated preferences
- Products should be in stock and properly priced

### ✅ Product Data Accuracy
- Prices match the database
- Stock numbers are real
- Ratings and reviews display correctly

### ✅ Conversation Context
- Bot maintains conversation history
- Same conversationId should preserve state
- Multiple queries in same conversation work properly

### ✅ Frontend Suggestions
- Quick suggestion buttons change based on context
- Input auto-focuses after bot response
- Messages display with timestamps

---

## Database Setup

If you need to add test products:

```sql
-- Insert sample perfumes
INSERT INTO products (name, brand, description, price, stock, category, type, volume, featured, active, rating, review_count)
VALUES 
  ('Gucci Bloom', 'Gucci', 'Floral fragrance with rose and jasmine notes', 3800.00, 38, 'Women', 'Eau de Toilette', 100, true, true, 4.5, 12),
  ('Dior Sauvage', 'Christian Dior', 'Fresh spicy fragrance for men', 4200.00, 25, 'Men', 'Eau de Toilette', 100, true, true, 4.8, 45),
  ('Rose Garden', 'Local Brand', 'Classic rose fragrance', 2800.00, 50, 'Women', 'Eau de Toilette', 100, true, true, 4.2, 8);
```

---

## Troubleshooting

### Chatbot Returns Generic Error
1. Check backend logs for errors
2. Verify database connection
3. Ensure ProductService is working

### Products Not Found
1. Check if products exist in database
2. Verify product names match exactly
3. Try partial names (bot does fuzzy matching)

### Conversation Not Persisting
1. Verify ConversationHistoryRepository is working
2. Check conversationId is being sent with each request
3. Look for database errors in logs

### Intent Not Detected Correctly
1. Check console for "Detected user intent:" message
2. Try rephrasing the query
3. Check IntentDetectionService regex patterns

---

## Performance Tuning

### For Production
```bash
# Build optimized JAR
mvn clean package -DskipTests -P prod

# Run with optimized settings
java -Xmx2g -Xms1g -XX:+UseG1GC \
  -jar target/perfume-shop-1.0.0.jar \
  --server.port=8080 \
  --spring.jpa.hibernate.ddl-auto=validate
```

### Frontend Optimization
```bash
cd frontend
npm run build
# Serve with nginx for production
```

---

## Monitoring

### Check Intent Distribution
```bash
# Query conversation history by intent
curl -X GET http://localhost:8080/api/conversation-history/by-intent?intent=PRICE_QUERY
```

### Monitor Chatbot Performance
- Check response times in browser DevTools
- Monitor backend CPU/memory usage
- Track database query times

---

## API Endpoints

### Chat Endpoint
```
POST /api/chatbot/chat
Content-Type: application/json

{
  "message": "User query here",
  "conversationId": "conv_xxxxx",
  "context": "{...}"  // Optional
}

Response:
{
  "status": "success",
  "message": "Bot response here",
  "conversationId": "conv_xxxxx"
}
```

### Get Conversation History
```
GET /api/conversation-history/{conversationId}
```

### Get Trending Products (for quick suggestions)
```
GET /api/products/trending?limit=5
```

---

## Example Conversation Flow

```
User: Hi
Bot: ✨ Welcome to Perfumé! I'm Sophia...
     What brings you here today?

User: What perfume for work?
Bot: 🎁 Perfect! For work, here are my recommendations:
     ✨ Dior Sauvage by Christian Dior
        💰 ₹4200 | ⭐ 4.8/5
     ... (more recommendations)

User: How much is Gucci Bloom?
Bot: 💰 Gucci Bloom - ₹3800
     ✅ In Stock - 38 units
     ... (more details)

User: Is it in stock?
Bot: ✅ Gucci Bloom is IN STOCK!
     📦 Available: 38 units
     ... (details)

User: Perfect! Tell me more
Bot: (Shows detailed information with description)
```

---

## Files to Review

### Backend
- `src/main/java/com/perfume/shop/service/IntentDetectionService.java` - Intent detection logic
- `src/main/java/com/perfume/shop/service/SmartRecommendationService.java` - Recommendation engine
- `src/main/java/com/perfume/shop/service/ChatbotService.java` - Main chatbot service (enhanced)
- `src/main/java/com/perfume/shop/entity/ConversationHistory.java` - Persistence model
- `src/main/java/com/perfume/shop/repository/ConversationHistoryRepository.java` - Database access

### Frontend
- `frontend/src/components/Chatbot.jsx` - Enhanced chatbot component
- `frontend/src/api/axios.js` - API configuration

---

## Next Steps

1. **Deploy to Production**
   - Set up database backups
   - Configure SSL/TLS
   - Set up monitoring

2. **Add Analytics**
   - Track popular queries
   - Monitor user satisfaction
   - Analyze intent patterns

3. **Enhance AI**
   - Train NLP model on conversation data
   - Implement machine learning recommendations
   - Add user feedback mechanism

4. **Extend Features**
   - Add wishlist support
   - Implement one-click checkout
   - Add social sharing

---

## Support

For issues or questions:
1. Check backend logs: `target/logs/`
2. Review browser console for frontend errors
3. Verify database connectivity
4. Check ConversationHistoryRepository queries

Happy selling! 🎁
