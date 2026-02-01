# Admin Panel - Code Architecture

## Component Hierarchy

```
AdminPanel (src/pages/AdminPanel.jsx)
├── Sidebar Navigation
│   ├── Logo & Toggle Button
│   └── Navigation Items
│       ├── Dashboard
│       ├── Products
│       ├── Orders
│       ├── Users
│       ├── Settings
│       └── Logout
├── Main Content Area
│   ├── Dashboard Section
│   │   └── Statistics Cards (4x)
│   ├── Products Section
│   │   ├── Header with Add Product Button
│   │   └── Products Table
│   │       ├── ID
│   │       ├── Name
│   │       ├── Category
│   │       ├── Price
│   │       ├── Stock
│   │       ├── Rating
│   │       └── Actions (Edit, Delete)
│   ├── Orders Section
│   │   ├── Header
│   │   └── Orders Table
│   │       ├── Order ID
│   │       ├── Customer
│   │       ├── Total
│   │       ├── Status Badge
│   │       ├── Date
│   │       └── Actions (View)
│   ├── Users Section
│   │   ├── Header
│   │   └── Users Table
│   │       ├── ID
│   │       ├── Email
│   │       ├── Name
│   │       ├── Role Badge
│   │       ├── Status
│   │       └── Actions (Edit)
│   └── Settings Section
│       └── Configuration Form
│           ├── Store Name
│           ├── Store Email
│           ├── Support Phone
│           ├── Currency
│           └── Save Button
```

## State Management

```javascript
// Component State (React Hooks)
const [activeTab, setActiveTab] = useState('dashboard')
const [sidebarOpen, setSidebarOpen] = useState(true)
const [products, setProducts] = useState([])
const [orders, setOrders] = useState([])
const [users, setUsers] = useState([])
const [loading, setLoading] = useState(false)
const [error, setError] = useState(null)
```

## Data Flow

```
User Login
    ↓
Store Token in localStorage
    ↓
Navigate to /admin
    ↓
Check Token (useEffect)
    ↓
Display Admin Panel
    ↓
User Selects Tab
    ↓
Fetch Data from API
    ↓
Update Component State
    ↓
Render Table/Cards
```

## CSS Architecture

### Structure
```
AdminPanel.css (464 lines)
├── Container Layout (10 lines)
├── Sidebar Styling (100 lines)
│   ├── Main sidebar
│   ├── Header
│   ├── Navigation items
│   └── Toggle button
├── Main Content Area (50 lines)
├── Dashboard Section (80 lines)
│   ├── Stats grid
│   └── Stat cards
├── Section Styling (100 lines)
│   ├── Tables
│   ├── Buttons
│   └── Forms
├── Components (100 lines)
│   ├── Badges
│   ├── Loading states
│   └── Empty states
└── Responsive Design (100 lines)
    └── Mobile breakpoints
```

### Color Scheme
```
Primary Gradient:     #667eea → #764ba2 (Purple)
Background:          #f5f7fa (Light Blue Gray)
Card Background:     #ffffff (White)
Text Primary:        #1a202c (Dark Gray)
Text Secondary:      #718096 (Medium Gray)
Border:              #e2e8f0 (Light Gray)
Success:             #22c55e (Green)
Warning:             #f59e0b (Amber)
Error:               #ef4444 (Red)
Info:                #3b82f6 (Blue)
```

## API Integration Points

### Products Endpoint
```javascript
async function fetchProducts() {
  const response = await fetch(
    'http://localhost:8080/api/products?size=100',
    { headers: { 'Authorization': `Bearer ${token}` } }
  );
  // Process response.json().content
}
```

### Orders Endpoint
```javascript
async function fetchOrders() {
  const response = await fetch(
    'http://localhost:8080/api/orders?size=50',
    { headers: { 'Authorization': `Bearer ${token}` } }
  );
  // Process response.json().content || response.json()
}
```

### Users Endpoint
```javascript
async function fetchUsers() {
  const response = await fetch(
    'http://localhost:8080/api/admin/users?size=50',
    { headers: { 'Authorization': `Bearer ${token}` } }
  );
  // Process response.json().content
}
```

## Hook Dependencies

```javascript
// Fetch data when tab changes
useEffect(() => {
  if (activeTab === 'products') fetchProducts();
  if (activeTab === 'orders') fetchOrders();
  if (activeTab === 'users') fetchUsers();
}, [activeTab])

// Check authentication on mount
useEffect(() => {
  const token = localStorage.getItem('token') || localStorage.getItem('accessToken');
  if (!token) navigate('/login');
}, [navigate])
```

## Component Features

### Authentication
- Checks for token in localStorage
- Redirects to login if not authenticated
- Uses both 'token' and 'accessToken' keys

### Tab Management
- Active tab state
- Tab-specific data fetching
- Conditional rendering based on activeTab

### Data Handling
- Loading states
- Error messages
- Empty states
- Data validation
- Pagination support

### UI Interactions
- Sidebar toggle
- Tab switching
- Logout button
- Action buttons (Edit, Delete, View)
- Button hover effects

## Error Handling Strategy

```javascript
// Try-Catch Pattern
try {
  const response = await fetch(url, options);
  if (response.ok) {
    const data = await response.json();
    setProducts(data.content || []);
  } else {
    throw new Error('Failed to fetch');
  }
} catch (err) {
  setError('Error loading products');
  console.error(err);
} finally {
  setLoading(false);
}
```

## Security Implementation

### Token Management
```javascript
// Get token from localStorage
const token = localStorage.getItem('token') || 
              localStorage.getItem('accessToken');

// Add to API headers
headers: { 'Authorization': `Bearer ${token}` }
```

### Logout Process
```javascript
function handleLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  navigate('/login');
}
```

## Responsive Design Approach

### Desktop (> 768px)
- Full 260px sidebar
- Labels visible on all nav items
- Full-width tables
- Grid layout for stats

### Mobile (< 768px)
- 80px collapsed sidebar
- Icons only navigation
- Horizontal nav items
- Responsive tables
- 2-column stat grid

## Performance Optimizations

1. **Data Fetching**
   - Fetch only when tab changes
   - Set loading state during fetch
   - Error handling prevents crashes

2. **Rendering**
   - Conditional rendering based on state
   - useEffect dependencies optimized
   - No unnecessary re-renders

3. **CSS**
   - CSS classes for styling
   - No inline styles
   - Smooth transitions (0.2-0.3s)

## Browser Compatibility

✅ Chrome 90+
✅ Firefox 88+
✅ Safari 14+
✅ Edge 90+
✅ Mobile browsers (iOS Safari, Chrome Mobile)

## Dependencies

### External Libraries
- react 18+
- react-router-dom 6+
- lucide-react (for icons)

### No Additional Dependencies Needed
- Uses native fetch API
- CSS Grid & Flexbox
- Native localStorage
- Native useEffect & useState
