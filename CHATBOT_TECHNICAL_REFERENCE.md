# Intelligent Chatbot - Technical Reference

## Service Dependencies Injection

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {
    private final ProductService productService;
    private final ConversationSessionManager conversationSessionManager;
    private final GeminiService geminiService;
    private final IntentDetectionService intentDetectionService;  // NEW
    private final SmartRecommendationService smartRecommendationService;  // NEW
    private final ConversationHistoryRepository conversationHistoryRepository;  // NEW
}
```

---

## Intent Detection Regex Patterns

```java
// Price Queries
".*(what|how much).*(?:price|cost|rupees|rs|₹).*"
".*(price|cost).*(?:of|for|gucci|dior|chanel|calvin|perfume).*"

// Availability
".*(available|in stock|stock|availability|get).*"
".*(is|are).*(?:available|in stock).*"

// Recommendations
".*(recommend|suggest|suggest me|what should|what would|best for).*"
".*(which|what).*(?:perfume|fragrance|scent).*(?:for|do you recommend).*"

// Comparisons
".*(compare|vs|versus|difference|better|which is better).*"

// Occasions
".*(occasion|event|wedding|date|party|work|professional|casual|romantic).*"

// Scent Types
".*(floral|woody|fresh|oriental|fruity|citrus|spicy|vanilla|musk|sandalwood|oud).*"

// Budget
".*(budget|under|below|less than|afford|price range|up to|between).*"
```

---

## Recommendation Scoring Algorithm

```java
private double scoreProduct(Product product, String occasion, String scentType, 
                           Integer budgetMin, Integer budgetMax, String userCategory) {
    double score = 0.0;
    
    // 1. Budget match (30%)
    if (productPrice >= budgetMin && productPrice <= budgetMax) {
        score += 30;
    } else if (productPrice < budgetMin) {
        score += 15;  // Partial credit
    }
    
    // 2. Category match (20%)
    if (product.getCategory().equalsIgnoreCase(userCategory)) {
        score += 20;
    } else if ("Unisex".equalsIgnoreCase(product.getCategory())) {
        score += 10;  // Unisex works for anyone
    }
    
    // 3. Fragrance notes match (25%)
    if (matchesScentType(scentType, product.getFragranceNotes())) {
        score += 25;
    } else {
        score += 12;  // Partial credit for having notes
    }
    
    // 4. Occasion match (15%)
    if (matchesOccasion(occasion, product.getDescription())) {
        score += 15;
    }
    
    // 5. Rating bonus (10%)
    score += (product.getRating() / 5.0) * 10;
    
    // 6. Popularity bonus (5%)
    if (product.getReviewCount() > 10) {
        score += 5;
    }
    
    return Math.min(score, 100);  // Cap at 100
}
```

---

## Request/Response Lifecycle

```
1. Frontend sends message
   POST /api/chatbot/chat
   {
     "message": "Gucci Bloom price",
     "conversationId": "conv_123"
   }

2. ChatbotController receives request
   → Generates conversationId if not provided
   → Calls chatbotService.chat()

3. ChatbotService processes message
   → Extract preferences from message
   → Detect intent using IntentDetectionService
   → Handle intent using appropriate handler
     - handleIntentPriceQuery()
     - handleIntentAvailabilityCheck()
     - handleIntentRecommendation()
     - etc.
   → Format response
   → Record in conversation history
   → Update session

4. Response returned to Frontend
   {
     "status": "success",
     "message": "💰 Gucci Bloom - ₹3800...",
     "conversationId": "conv_123"
   }

5. Frontend displays message
   → Clears input
   → Appends bot message to chat
   → Auto-focuses input
   → Scrolls to bottom
```

---

## Database Schema

### ConversationHistory Table
```sql
CREATE TABLE conversation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    user_message TEXT NOT NULL,
    bot_response TEXT NOT NULL,
    user_intent VARCHAR(50),
    context TEXT,
    message_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_resolved BOOLEAN DEFAULT FALSE,
    metadata VARCHAR(500),
    
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
);
```

---

## Scent Type Keyword Mapping

```java
Map<String, List<String>> scentKeywords = Map.ofEntries(
    Map.entry("floral", 
        Arrays.asList("rose", "jasmine", "lily", "peony", "gardenia", "mimosa", "violet")),
    
    Map.entry("woody", 
        Arrays.asList("sandalwood", "cedarwood", "oud", "vetiver", "iris", "musk", "amber")),
    
    Map.entry("fresh", 
        Arrays.asList("citrus", "lemon", "bergamot", "grapefruit", "lime", "mint", "green", "aqua")),
    
    Map.entry("oriental", 
        Arrays.asList("vanilla", "amber", "incense", "patchouli", "tonka", "musk", "powdery")),
    
    Map.entry("fruity", 
        Arrays.asList("apple", "peach", "plum", "berry", "strawberry", "raspberry", "pear", "mango")),
    
    Map.entry("spicy", 
        Arrays.asList("cinnamon", "pepper", "clove", "nutmeg", "cardamom", "ginger")),
    
    Map.entry("citrus", 
        Arrays.asList("lemon", "orange", "citrus", "bergamot", "lime", "grapefruit"))
);
```

---

## Occasion Keyword Mapping

```java
Map<String, List<String>> occasionKeywords = Map.ofEntries(
    Map.entry("work", 
        Arrays.asList("professional", "office", "workplace", "business", "fresh", "light")),
    
    Map.entry("romantic", 
        Arrays.asList("romantic", "date", "sensual", "seductive", "intimate", "passion")),
    
    Map.entry("casual", 
        Arrays.asList("casual", "everyday", "daily", "light", "fresh", "comfortable")),
    
    Map.entry("party", 
        Arrays.asList("party", "evening", "night", "celebration", "bold", "intense")),
    
    Map.entry("wedding", 
        Arrays.asList("wedding", "celebration", "special", "elegant", "premium", "luxur")),
    
    Map.entry("beach", 
        Arrays.asList("beach", "summer", "fresh", "citrus", "light", "marine"))
);
```

---

## Intent Handler Methods

### Price Query Handler
```java
private String handleIntentPriceQuery(String userMessage, ConversationContext session) {
    // 1. Extract product name
    String productName = intentDetectionService.extractProductName(userMessage);
    
    // 2. Search database
    var product = productService.findProductByName(productName);
    if (product == null) {
        product = productService.findProductByNameContains(productName);
    }
    
    // 3. Format response
    if (product != null) {
        return "💰 **" + product.getName() + "** costs **₹" + product.getPrice() + "**\n\n" +
               "✅ **In Stock**: " + product.getStock() + " units\n" +
               "🏷️ **Brand**: " + product.getBrand() + "\n" +
               "⭐ **Rating**: " + String.format("%.1f", product.getRating()) + "/5";
    }
    
    return "I couldn't find that product.";
}
```

### Recommendation Handler
```java
private String handleIntentRecommendation(String userMessage, ConversationContext session) {
    // 1. Extract preferences
    String occasion = extractOccasion(userMessage);
    String scentType = extractScentType(userMessage);
    int[] budgetRange = intentDetectionService.extractBudgetRange(userMessage);
    
    // 2. Get recommendations
    var recommendations = smartRecommendationService.getRecommendations(
        occasion, scentType, 
        budgetRange != null ? budgetRange[0] : null,
        budgetRange != null ? budgetRange[1] : 10000,
        null, 5
    );
    
    // 3. Format response
    if (!recommendations.isEmpty()) {
        String response = "🎁 **Smart Recommendations**:\n\n";
        for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
            var rec = recommendations.get(i);
            response += "✨ **" + rec.getName() + "** by " + rec.getBrand() + "\n" +
                       "   💰 ₹" + rec.getPrice() + " | ⭐ " + 
                       String.format("%.1f", rec.getRating()) + "/5\n";
        }
        return response;
    }
    
    return "I'd love to help! Tell me more about your preferences.";
}
```

---

## Error Handling

```java
try {
    // Intent detection always succeeds (defaults to GENERAL_CHAT)
    UserIntent intent = intentDetectionService.detectIntent(userMessage);
    
    // Handle intent (returns null if not handled)
    String intentResponse = handleIntentBasedQuery(userMessage, intent, session);
    if (intentResponse != null) {
        return recordMessageAndReturn(conversationId, userMessage, intentResponse, session);
    }
    
    // Fallback to existing direct product lookup
    String directResponse = tryDirectProductLookup(userMessage, session);
    if (directResponse != null) {
        return recordMessageAndReturn(conversationId, userMessage, directResponse, session);
    }
    
    // Ultimate fallback to AI or rule-based
    String aiResponse = geminiService.generateResponse(userMessage, session);
    if (aiResponse != null) {
        return recordMessageAndReturn(conversationId, userMessage, aiResponse, session);
    }
    
    // Rule-based conversation flow
    String response = handleConversationStage(userMessage, session.getCurrentStage(), session);
    return recordMessageAndReturn(conversationId, userMessage, response, session);
    
} catch (Exception e) {
    log.error("Error in chatbot service", e);
    return "I apologize, but I encountered an error. Please try again.";
}
```

---

## Frontend Message Structure

```jsx
const message = {
    id: 1,
    type: 'user' | 'bot',
    text: 'Message content here',
    timestamp: new Date()
};

// User messages: Right-aligned, pink background
// Bot messages: Left-aligned, white background with border
```

---

## Conversation Context Management

```java
// Initialize conversation
ConversationContext session = conversationSessionManager.getOrCreateSession(conversationId);

// Extract and store preferences
extractAndStorePreferences(userMessage, session);

// Get current preferences
String occasion = session.getPreference("occasion");
String scentType = session.getPreference("scent_type");
String budget = session.getPreference("budget");

// Track what's been asked
session.markQuestionAsked("occasion");
if (!session.isQuestionAsked("occasion")) {
    // Ask for occasion
}

// Store recommendations
session.addRecommendation("Gucci Bloom", productId);
List<String> recommendations = session.getStoredRecommendations();
Long productId = session.getProductIdByName("Gucci Bloom");

// Update session
conversationSessionManager.updateSession(conversationId, session);
```

---

## Performance Optimization Tips

### 1. Cache Product List
```java
@Cacheable("products")
public List<Product> getAllProducts() {
    return productRepository.findAll();
}
```

### 2. Query Optimization
```java
// Instead of lazy loading all associations:
@Query("SELECT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.active = true")
List<Product> getAllActiveProductsWithReviews();
```

### 3. Pagination for Large Result Sets
```java
public Page<ProductResponse> getProductsByPriceRange(
    int minPrice, int maxPrice, Pageable pageable) {
    return productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
}
```

### 4. Add Indexes for Frequently Queried Fields
```java
@Index(name = "idx_product_price", columnList = "price"),
@Index(name = "idx_product_rating", columnList = "rating"),
@Index(name = "idx_conversation_created_at", columnList = "created_at")
```

---

## Testing Strategies

### Unit Test Example
```java
@Test
public void testIntentDetection() {
    String message = "What's the price of Gucci Bloom?";
    UserIntent intent = intentDetectionService.detectIntent(message);
    assertEquals(UserIntent.PRICE_QUERY, intent);
    
    String productName = intentDetectionService.extractProductName(message);
    assertEquals("Gucci Bloom", productName);
}
```

### Integration Test Example
```java
@Test
public void testPriceQueryFlow() {
    String response = chatbotService.chat("Gucci Bloom price", "conv-1", null);
    assertTrue(response.contains("₹"));
    assertTrue(response.contains("Gucci Bloom"));
    assertTrue(response.contains("Stock"));
}
```

### Frontend Test Example
```jsx
it('detects price query intent', async () => {
    const { getByPlaceholderText, getByText } = render(<Chatbot />);
    const input = getByPlaceholderText(/Ask about/i);
    
    fireEvent.change(input, { target: { value: 'Gucci Bloom price' } });
    fireEvent.click(getByText(/Send/i));
    
    await waitFor(() => {
        expect(getByText(/₹/)).toBeInTheDocument();
    });
});
```

---

## Migration Guide (If Upgrading)

### Step 1: Run Database Migrations
```sql
-- Create conversation_history table
CREATE TABLE conversation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    user_message TEXT NOT NULL,
    bot_response TEXT NOT NULL,
    user_intent VARCHAR(50),
    context TEXT,
    message_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_resolved BOOLEAN DEFAULT FALSE,
    metadata VARCHAR(500),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
);
```

### Step 2: Deploy New Services
- Add IntentDetectionService to classpath
- Add SmartRecommendationService to classpath
- Add ConversationHistory entity
- Update ChatbotService with new dependencies

### Step 3: Update Frontend
- Replace Chatbot.jsx with new version
- Clear browser cache

### Step 4: Test
- Test all intent types
- Verify recommendations
- Check conversation history

---

This completes the **Intelligent Shopping Assistant Chatbot** implementation.

**Version:** 2.0
**Status:** Production Ready
**Last Updated:** February 2, 2026
