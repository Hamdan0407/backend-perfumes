import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BarChart3, Package, ShoppingCart, Users, Settings, LogOut, Menu, X,
  TrendingUp, DollarSign, Eye, Edit, Trash2, Plus, Search, Bell,
  Calendar, ArrowUpRight, RefreshCw, Save, XCircle, Check, AlertCircle,
  Download, Upload, FileText, Image as ImageIcon
} from 'lucide-react';
import { jsPDF } from 'jspdf';
import { useAuthStore } from '../store/authStore';
import { toast } from 'react-toastify';
import api from '../api/axios';
import '../styles/AdminPanel.css';

export default function AdminPanel() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [lastUpdated, setLastUpdated] = useState(new Date());
  const [isLive, setIsLive] = useState(true);
  const [showNotifications, setShowNotifications] = useState(false);
  const [notifications, setNotifications] = useState([]);
  
  // Modal states
  const [showProductModal, setShowProductModal] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [showUserModal, setShowUserModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [modalMode, setModalMode] = useState('add'); // 'add' or 'edit'

  // Product form
  const [productForm, setProductForm] = useState({
    name: '',
    description: '',
    price: '',
    stock: '',
    category: 'Floral',
    brand: '',
    imageUrl: '',
    size: '100ml',
    type: 'Eau de Parfum',
    active: true
  });
  
  // Image upload
  const [imagePreview, setImagePreview] = useState(null);
  const [uploadMethod, setUploadMethod] = useState('url'); // 'url' or 'upload'
  const fileInputRef = useRef(null);

  // Settings form
  const [settingsForm, setSettingsForm] = useState({
    storeName: 'Perfume Shop',
    storeEmail: 'admin@perfumeshop.com',
    supportPhone: '+91 98765 43210',
    currency: 'INR',
    taxRate: '18',
    freeShippingThreshold: '2000',
    defaultShippingCost: '99'
  });

  // Format price in INR
  const formatINR = (amount) => {
    if (!amount && amount !== 0) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  };
  
  // Get status color for badges
  const getStatusColor = (status) => {
    const colors = {
      'PLACED': 'warning',
      'CONFIRMED': 'info',
      'PACKED': 'info',
      'SHIPPED': 'primary',
      'DELIVERED': 'success',
      'COMPLETED': 'success',
      'CANCELLED': 'danger',
      'REFUNDED': 'secondary'
    };
    return colors[status] || 'default';
  };

  // Calculate real-time analytics (memoized)
  const analytics = React.useMemo(() => {
    // Total Revenue (from completed/delivered orders)
    const completedOrders = orders.filter(o => 
      ['DELIVERED', 'COMPLETED', 'SHIPPED'].includes(o.status)
    );
    const totalRevenue = orders.reduce((sum, order) => sum + (order.totalAmount || 0), 0);
    const confirmedRevenue = completedOrders.reduce((sum, order) => sum + (order.totalAmount || 0), 0);

    // Order stats - using backend enum values
    const pendingOrders = orders.filter(o => o.status === 'PLACED').length;
    const processingOrders = orders.filter(o => ['PACKED', 'CONFIRMED'].includes(o.status)).length;
    const shippedOrders = orders.filter(o => o.status === 'SHIPPED').length;
    const deliveredOrders = orders.filter(o => o.status === 'DELIVERED').length;
    const cancelledOrders = orders.filter(o => o.status === 'CANCELLED').length;

    // Stock stats
    const totalStock = products.reduce((sum, p) => sum + (p.stock || 0), 0);
    const lowStockProducts = products.filter(p => (p.stock || 0) <= 10 && (p.stock || 0) > 0);
    const outOfStockProducts = products.filter(p => (p.stock || 0) === 0);
    const activeProducts = products.filter(p => p.active !== false);

    // Customer stats
    const totalCustomers = users.filter(u => u.role === 'CUSTOMER' || !u.role).length;
    const activeCustomers = users.filter(u => u.active !== false).length;

    // Today's stats
    const today = new Date().toDateString();
    const todayOrders = orders.filter(o => {
      if (!o.createdAt) return false;
      return new Date(o.createdAt).toDateString() === today;
    });
    const todayRevenue = todayOrders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);

    // Average order value
    const avgOrderValue = orders.length > 0 ? totalRevenue / orders.length : 0;

    // Inventory value
    const inventoryValue = products.reduce((sum, p) => sum + ((p.price || 0) * (p.stock || 0)), 0);

    return {
      totalRevenue,
      confirmedRevenue,
      pendingOrders,
      processingOrders,
      shippedOrders,
      deliveredOrders,
      cancelledOrders,
      totalStock,
      lowStockProducts,
      outOfStockProducts,
      activeProducts,
      totalCustomers,
      activeCustomers,
      todayOrders: todayOrders.length,
      todayRevenue,
      avgOrderValue,
      inventoryValue
    };
  }, [orders, products, users]);

  // Categories for dropdown
  const categories = ['Floral', 'Woody', 'Oriental', 'Fresh', 'Citrus', 'Aromatic', 'Gourmand', 'Aquatic'];
  const orderStatuses = ['PLACED', 'CONFIRMED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

  // Fetch Products
  const fetchProducts = React.useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/products?size=100');
      setProducts(data.content || data || []);
    } catch (err) {
      console.error('Error loading products:', err);
      toast.error('Failed to load products');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch Orders
  const fetchOrders = React.useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/admin/orders?size=100');
      setOrders(data.content || data || []);
    } catch (err) {
      console.error('Error loading orders:', err);
      // Try alternate endpoint
      try {
        const { data } = await api.get('/orders?size=100');
        setOrders(data.content || data || []);
      } catch (e) {
        console.error('Alternate orders endpoint failed:', e);
        toast.error('Failed to load orders');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch Users
  const fetchUsers = React.useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/admin/users?size=100');
      setUsers(data.content || data || []);
    } catch (err) {
      console.error('Error loading users:', err);
      toast.error('Failed to load users');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch all data on mount
  useEffect(() => {
    fetchProducts();
    fetchOrders();
    fetchUsers();
  }, []);

  // Real-time auto-refresh every 10 seconds for dashboard
  useEffect(() => {
    if (activeTab === 'dashboard' && isLive) {
      const interval = setInterval(async () => {
        // Silent refresh without loading spinner
        try {
          const [productsRes, ordersRes, usersRes] = await Promise.allSettled([
            api.get('/products?size=100'),
            api.get('/admin/orders?size=100').catch(() => api.get('/orders?size=100')),
            api.get('/admin/users?size=100')
          ]);
          
          if (productsRes.status === 'fulfilled') {
            setProducts(productsRes.value.data.content || productsRes.value.data || []);
          }
          if (ordersRes.status === 'fulfilled') {
            setOrders(ordersRes.value.data.content || ordersRes.value.data || []);
          }
          if (usersRes.status === 'fulfilled') {
            setUsers(usersRes.value.data.content || usersRes.value.data || []);
          }
          
          setLastUpdated(new Date());
        } catch (error) {
          console.error('Auto-refresh error:', error);
        }
      }, 10000); // Refresh every 10 seconds

      return () => clearInterval(interval);
    }
  }, [activeTab, isLive]);

  useEffect(() => {
    if (activeTab === 'products') fetchProducts();
    if (activeTab === 'orders') fetchOrders();
    if (activeTab === 'users') fetchUsers();
  }, [activeTab, fetchProducts, fetchOrders, fetchUsers]);

  const handleLogout = React.useCallback(() => {
    logout();
    navigate('/login');
  }, [logout, navigate]);

  const refreshAll = React.useCallback(() => {
    fetchProducts();
    fetchOrders();
    fetchUsers();
    setLastUpdated(new Date());
    toast.success('Data refreshed!');
  }, [fetchProducts, fetchOrders, fetchUsers]);

  // ==================== PRODUCT OPERATIONS ====================
  
  const openAddProductModal = () => {
    setModalMode('add');
    setProductForm({
      name: '',
      description: '',
      price: '',
      stock: '',
      category: 'Floral',
      brand: '',
      imageUrl: '',
      size: '100ml',
      type: 'Eau de Parfum',
      active: true
    });
    setImagePreview(null);
    setUploadMethod('url');
    setShowProductModal(true);
  };

  const openEditProductModal = (product) => {
    setModalMode('edit');
    setSelectedItem(product);
    setProductForm({
      name: product.name || '',
      description: product.description || '',
      price: product.price?.toString() || '',
      stock: product.stock?.toString() || '',
      category: product.category || 'Floral',
      brand: product.brand || '',
      imageUrl: product.imageUrl || '',
      size: product.size || '100ml',
      type: product.type || 'Eau de Parfum',
      active: product.active !== false
    });
    // Set image preview if product has an image
    if (product.imageUrl) {
      setImagePreview(product.imageUrl);
      setUploadMethod(product.imageUrl.startsWith('data:') ? 'upload' : 'url');
    } else {
      setImagePreview(null);
      setUploadMethod('url');
    }
    setShowProductModal(true);
  };

  const handleProductSubmit = React.useCallback(async (e) => {
    e.preventDefault();
    setLoading(true);
    
    // Build product data with proper types
    const imageUrl = productForm.imageUrl || '';
    const productData = {
      name: productForm.name.trim(),
      description: productForm.description?.trim() || 'Premium perfume',
      price: parseFloat(productForm.price) || 0,
      stock: parseInt(productForm.stock, 10) || 0,
      category: productForm.category || 'Floral',
      brand: productForm.brand?.trim() || 'Generic',
      imageUrl: imageUrl.startsWith('data:') ? imageUrl : imageUrl.trim(),
      size: productForm.size || '100ml',
      type: productForm.type || 'Eau de Parfum',
      active: productForm.active !== false,
      featured: false
    };

    try {
      if (modalMode === 'add') {
        await api.post('/admin/products', productData);
        toast.success('Product created successfully!');
      } else {
        await api.put(`/admin/products/${selectedItem.id}`, productData);
        toast.success('Product updated successfully!');
      }
      setShowProductModal(false);
      fetchProducts();
    } catch (err) {
      console.error('Product save error:', err);
      const errorMsg = err.response?.data?.message 
        || err.response?.data?.error 
        || (typeof err.response?.data === 'string' ? err.response?.data : null)
        || 'Failed to save product';
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  }, [productForm, modalMode, selectedItem, fetchProducts]);

  const confirmDeleteProduct = (product) => {
    setSelectedItem(product);
    setShowDeleteConfirm(true);
  };

  const handleDeleteProduct = async () => {
    setLoading(true);
    try {
      await api.delete(`/admin/products/${selectedItem.id}`);
      toast.success('Product deleted successfully!');
      setShowDeleteConfirm(false);
      fetchProducts();
    } catch (err) {
      console.error('Delete error:', err);
      toast.error('Failed to delete product');
    } finally {
      setLoading(false);
    }
  };

  // ==================== ORDER OPERATIONS ====================
  
  const openOrderModal = (order) => {
    setSelectedItem(order);
    setShowOrderModal(true);
  };

  const handleUpdateOrderStatus = async (orderId, newStatus) => {
    try {
      await api.put(`/admin/orders/${orderId}/status`, { status: newStatus });
      toast.success(`Order status updated to ${newStatus}`);
      fetchOrders();
      setShowOrderModal(false);
    } catch (err) {
      console.error('Order update error:', err);
      toast.error('Failed to update order status');
    }
  };

  // ==================== USER OPERATIONS ====================
  
  const openUserModal = (user) => {
    setSelectedItem(user);
    setShowUserModal(true);
  };

  const handleToggleUserStatus = async (userId, currentStatus) => {
    try {
      await api.put(`/admin/users/${userId}/status`, { active: !currentStatus });
      toast.success(`User ${currentStatus ? 'deactivated' : 'activated'} successfully`);
      fetchUsers();
    } catch (err) {
      console.error('User status update error:', err);
      toast.error('Failed to update user status');
    }
  };

  const handleChangeUserRole = async (userId, newRole) => {
    try {
      await api.put(`/admin/users/${userId}/role`, { role: newRole });
      toast.success(`User role updated to ${newRole}`);
      fetchUsers();
      setShowUserModal(false);
    } catch (err) {
      console.error('Role update error:', err);
      toast.error('Failed to update user role');
    }
  };

  // ==================== SETTINGS OPERATIONS ====================
  
  const handleSaveSettings = React.useCallback(() => {
    // Save to localStorage for now (in production, this would go to backend)
    localStorage.setItem('storeSettings', JSON.stringify(settingsForm));
    toast.success('Settings saved successfully!');
  }, [settingsForm]);

  // Load settings from localStorage
  useEffect(() => {
    const savedSettings = localStorage.getItem('storeSettings');
    if (savedSettings) {
      try {
        setSettingsForm(JSON.parse(savedSettings));
      } catch (error) {
        console.error('Failed to parse saved settings:', error);
      }
    }
  }, []);

  // ==================== IMAGE UPLOAD HANDLER ====================
  const handleImageUpload = React.useCallback((e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    // Validate file type
    if (!file.type.startsWith('image/')) {
      toast.error('Please select an image file');
      return;
    }
    
    // Validate file size (max 5MB)
    const MAX_SIZE = 5 * 1024 * 1024;
    if (file.size > MAX_SIZE) {
      toast.error('Image size must be less than 5MB');
      return;
    }
    
    // Convert to base64 for preview and storage
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64String = reader.result;
      setImagePreview(base64String);
      setProductForm(prev => ({...prev, imageUrl: base64String}));
      toast.success('Image uploaded successfully!');
    };
    reader.onerror = () => {
      toast.error('Failed to read image file');
    };
    reader.readAsDataURL(file);
  }, []);

  const removeImage = React.useCallback(() => {
    setImagePreview(null);
    setProductForm(prev => ({...prev, imageUrl: ''}));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, []);

  // ==================== INVOICE PDF GENERATOR ====================
  const generateInvoicePDF = (order) => {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    
    // Colors
    const primaryColor = [139, 92, 246]; // Purple
    const darkColor = [30, 41, 59];
    const grayColor = [100, 116, 139];
    
    // Header Background
    doc.setFillColor(...primaryColor);
    doc.rect(0, 0, pageWidth, 45, 'F');
    
    // Company Name
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(28);
    doc.setFont('helvetica', 'bold');
    doc.text('PERFUME SHOP', 20, 25);
    
    // Invoice Label
    doc.setFontSize(12);
    doc.setFont('helvetica', 'normal');
    doc.text('TAX INVOICE', 20, 35);
    
    // Invoice Number (right side)
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text(`#INV-${order.id?.toString().padStart(5, '0')}`, pageWidth - 20, 25, { align: 'right' });
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.text(new Date(order.createdAt || Date.now()).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    }), pageWidth - 20, 35, { align: 'right' });
    
    // Reset text color
    doc.setTextColor(...darkColor);
    
    // Bill To Section
    let yPos = 60;
    doc.setFontSize(10);
    doc.setTextColor(...grayColor);
    doc.text('BILL TO', 20, yPos);
    
    yPos += 8;
    doc.setTextColor(...darkColor);
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text(order.customerName || order.user?.firstName || 'Customer', 20, yPos);
    
    yPos += 7;
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    if (order.shippingAddress) {
      const address = order.shippingAddress;
      doc.text(`${address.street || ''}`, 20, yPos);
      yPos += 5;
      doc.text(`${address.city || ''}, ${address.state || ''} ${address.zipCode || ''}`, 20, yPos);
      yPos += 5;
      doc.text(`${address.country || 'India'}`, 20, yPos);
    }
    
    // Order Status (right side)
    doc.setFontSize(10);
    doc.setTextColor(...grayColor);
    doc.text('ORDER STATUS', pageWidth - 60, 60);
    doc.setTextColor(...primaryColor);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text(order.status || 'PENDING', pageWidth - 60, 70);
    
    // Items Table Header
    yPos = 110;
    doc.setFillColor(248, 250, 252);
    doc.rect(15, yPos - 5, pageWidth - 30, 12, 'F');
    
    doc.setTextColor(...grayColor);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('ITEM', 20, yPos + 3);
    doc.text('QTY', 110, yPos + 3);
    doc.text('PRICE', 135, yPos + 3);
    doc.text('TOTAL', pageWidth - 25, yPos + 3, { align: 'right' });
    
    // Items
    yPos += 18;
    doc.setTextColor(...darkColor);
    doc.setFont('helvetica', 'normal');
    
    const items = order.items || order.orderItems || [];
    if (items.length > 0) {
      items.forEach((item, index) => {
        const itemName = item.productName || item.product?.name || `Product ${index + 1}`;
        const qty = item.quantity || 1;
        const price = item.price || item.product?.price || 0;
        const total = qty * price;
        
        doc.setFontSize(11);
        doc.text(itemName.substring(0, 40), 20, yPos);
        doc.text(qty.toString(), 115, yPos);
        doc.text(`₹${price.toLocaleString('en-IN')}`, 135, yPos);
        doc.text(`₹${total.toLocaleString('en-IN')}`, pageWidth - 25, yPos, { align: 'right' });
        yPos += 10;
      });
    } else {
      doc.text('Order items', 20, yPos);
      doc.text('1', 115, yPos);
      doc.text(`₹${(order.totalAmount || 0).toLocaleString('en-IN')}`, 135, yPos);
      doc.text(`₹${(order.totalAmount || 0).toLocaleString('en-IN')}`, pageWidth - 25, yPos, { align: 'right' });
      yPos += 10;
    }
    
    // Divider Line
    yPos += 5;
    doc.setDrawColor(226, 232, 240);
    doc.line(15, yPos, pageWidth - 15, yPos);
    
    // Summary Section
    yPos += 15;
    const subtotal = order.totalAmount || 0;
    const shipping = order.shippingCost || 0;
    const tax = Math.round(subtotal * 0.18); // 18% GST
    const grandTotal = subtotal;
    
    doc.setFontSize(10);
    doc.setTextColor(...grayColor);
    doc.text('Subtotal:', pageWidth - 80, yPos);
    doc.setTextColor(...darkColor);
    doc.text(`₹${(subtotal - tax).toLocaleString('en-IN')}`, pageWidth - 25, yPos, { align: 'right' });
    
    yPos += 8;
    doc.setTextColor(...grayColor);
    doc.text('GST (18%):', pageWidth - 80, yPos);
    doc.setTextColor(...darkColor);
    doc.text(`₹${tax.toLocaleString('en-IN')}`, pageWidth - 25, yPos, { align: 'right' });
    
    yPos += 8;
    doc.setTextColor(...grayColor);
    doc.text('Shipping:', pageWidth - 80, yPos);
    doc.setTextColor(...darkColor);
    doc.text(shipping > 0 ? `₹${shipping.toLocaleString('en-IN')}` : 'FREE', pageWidth - 25, yPos, { align: 'right' });
    
    // Grand Total
    yPos += 15;
    doc.setFillColor(...primaryColor);
    doc.rect(pageWidth - 100, yPos - 7, 85, 18, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text('TOTAL:', pageWidth - 95, yPos + 4);
    doc.text(`₹${grandTotal.toLocaleString('en-IN')}`, pageWidth - 20, yPos + 4, { align: 'right' });
    
    // Footer
    doc.setTextColor(...grayColor);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    const footerY = doc.internal.pageSize.getHeight() - 25;
    doc.text('Thank you for shopping with Perfume Shop!', pageWidth / 2, footerY, { align: 'center' });
    doc.text('For queries: support@perfumeshop.com | +91 98765 43210', pageWidth / 2, footerY + 6, { align: 'center' });
    doc.text('This is a computer generated invoice', pageWidth / 2, footerY + 12, { align: 'center' });
    
    // Save the PDF
    doc.save(`Invoice-${order.id}.pdf`);
    toast.success('Invoice downloaded successfully!');
  };

  // Memoized filtered data
  const filteredProducts = React.useMemo(() => {
    if (!searchQuery) return products;
    const query = searchQuery.toLowerCase();
    return products.filter(p => 
      p.name?.toLowerCase().includes(query) ||
      p.category?.toLowerCase().includes(query) ||
      p.brand?.toLowerCase().includes(query)
    );
  }, [products, searchQuery]);

  const filteredOrders = React.useMemo(() => {
    if (!searchQuery) return orders;
    const query = searchQuery.toLowerCase();
    return orders.filter(o =>
      o.id?.toString().includes(searchQuery) ||
      o.customerName?.toLowerCase().includes(query) ||
      o.status?.toLowerCase().includes(query)
    );
  }, [orders, searchQuery]);

  const filteredUsers = React.useMemo(() => {
    if (!searchQuery) return users;
    const query = searchQuery.toLowerCase();
    return users.filter(u =>
      u.email?.toLowerCase().includes(query) ||
      u.firstName?.toLowerCase().includes(query) ||
      u.lastName?.toLowerCase().includes(query)
    );
  }, [users, searchQuery]);

  // Generate notifications (memoized)
  const currentNotifications = React.useMemo(() => {
    const notifs = [];
    
    // Pending orders
    if (analytics.pendingOrders > 0) {
      notifs.push({
        id: 'pending-orders',
        type: 'warning',
        icon: '⏳',
        title: `${analytics.pendingOrders} Pending Order${analytics.pendingOrders > 1 ? 's' : ''}`,
        message: 'Orders awaiting confirmation',
        action: () => { setActiveTab('orders'); setSearchQuery('PENDING'); setShowNotifications(false); }
      });
    }
    
    // Processing orders
    if (analytics.processingOrders > 0) {
      notifs.push({
        id: 'processing-orders',
        type: 'info',
        icon: '📦',
        title: `${analytics.processingOrders} Processing`,
        message: 'Orders being prepared',
        action: () => { setActiveTab('orders'); setSearchQuery('PROCESSING'); setShowNotifications(false); }
      });
    }
    
    // Low stock products
    if (analytics.lowStockProducts.length > 0) {
      notifs.push({
        id: 'low-stock',
        type: 'warning',
        icon: '⚠️',
        title: `${analytics.lowStockProducts.length} Low Stock Items`,
        message: analytics.lowStockProducts.slice(0, 2).map(p => p.name).join(', ') + 
                 (analytics.lowStockProducts.length > 2 ? '...' : ''),
        action: () => { setActiveTab('products'); setShowNotifications(false); }
      });
    }
    
    // Out of stock products
    if (analytics.outOfStockProducts.length > 0) {
      notifs.push({
        id: 'out-of-stock',
        type: 'danger',
        icon: '🚫',
        title: `${analytics.outOfStockProducts.length} Out of Stock`,
        message: 'Products need restocking immediately',
        action: () => { setActiveTab('products'); setShowNotifications(false); }
      });
    }
    
    // Shipped orders
    if (analytics.shippedOrders > 0) {
      notifs.push({
        id: 'shipped',
        type: 'success',
        icon: '🚚',
        title: `${analytics.shippedOrders} Order${analytics.shippedOrders > 1 ? 's' : ''} Shipped`,
        message: 'On the way to customers',
        action: () => { setActiveTab('orders'); setSearchQuery('SHIPPED'); setShowNotifications(false); }
      });
    }
    
    // Today's performance
    if (analytics.todayOrders > 0) {
      notifs.push({
        id: 'today',
        type: 'success',
        icon: '📈',
        title: `${analytics.todayOrders} Orders Today`,
        message: `Revenue: ${formatINR(analytics.todayRevenue)}`,
        action: () => { setActiveTab('dashboard'); setShowNotifications(false); }
      });
    }
    
    return notifs;
  }, [analytics, formatINR]);

  const notificationCount = React.useMemo(() => 
    currentNotifications.filter(n => n.type === 'warning' || n.type === 'danger').length,
    [currentNotifications]
  );

  // Get search result count based on active tab
  const getSearchResultCount = () => {
    if (!searchQuery) return null;
    switch (activeTab) {
      case 'products': return filteredProducts.length;
      case 'orders': return filteredOrders.length;
      case 'users': return filteredUsers.length;
      default: return null;
    }
  };

  // Clear search
  const clearSearch = () => {
    setSearchQuery('');
  };

  return (
    <div className="admin-layout">
      {/* Sidebar */}
      <aside className={`admin-sidebar ${sidebarOpen ? '' : 'collapsed'}`}>
        <div className="sidebar-brand">
          <div className="brand-logo">
            <div className="logo-icon">
              <BarChart3 size={24} />
            </div>
            {sidebarOpen && <span className="brand-text">PerfumeAdmin</span>}
          </div>
          <button className="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>
            {sidebarOpen ? <X size={18} /> : <Menu size={18} />}
          </button>
        </div>

        <nav className="sidebar-nav">
          <div className="nav-section">
            {sidebarOpen && <span className="nav-section-title">Main Menu</span>}
            
            <button
              className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
              onClick={() => setActiveTab('dashboard')}
            >
              <BarChart3 size={20} />
              {sidebarOpen && <span>Dashboard</span>}
            </button>
            
            <button
              className={`nav-item ${activeTab === 'products' ? 'active' : ''}`}
              onClick={() => setActiveTab('products')}
            >
              <Package size={20} />
              {sidebarOpen && <span>Products</span>}
              {sidebarOpen && <span className="nav-badge">{products.length}</span>}
            </button>
            
            <button
              className={`nav-item ${activeTab === 'orders' ? 'active' : ''}`}
              onClick={() => setActiveTab('orders')}
            >
              <ShoppingCart size={20} />
              {sidebarOpen && <span>Orders</span>}
              {analytics.pendingOrders > 0 && sidebarOpen && (
                <span className="nav-badge warning">{analytics.pendingOrders}</span>
              )}
            </button>
            
            <button
              className={`nav-item ${activeTab === 'users' ? 'active' : ''}`}
              onClick={() => setActiveTab('users')}
            >
              <Users size={20} />
              {sidebarOpen && <span>Customers</span>}
            </button>
          </div>

          <div className="nav-section">
            {sidebarOpen && <span className="nav-section-title">Settings</span>}
            
            <button
              className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`}
              onClick={() => setActiveTab('settings')}
            >
              <Settings size={20} />
              {sidebarOpen && <span>Settings</span>}
            </button>
          </div>
        </nav>

        <div className="sidebar-footer">
          <div className="user-profile">
            <div className="user-avatar">
              {user?.firstName?.charAt(0) || 'A'}
            </div>
            {sidebarOpen && (
              <div className="user-info">
                <span className="user-name">{user?.firstName || 'Admin'}</span>
                <span className="user-role">Administrator</span>
              </div>
            )}
          </div>
          <button className="nav-item logout" onClick={handleLogout}>
            <LogOut size={20} />
            {sidebarOpen && <span>Sign Out</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="admin-main">
        {/* Top Bar */}
        <header className="admin-topbar">
          <div className="topbar-left">
            <h1 className="page-title">
              {activeTab === 'dashboard' && '📊 Dashboard'}
              {activeTab === 'products' && '📦 Products'}
              {activeTab === 'orders' && '🛒 Orders'}
              {activeTab === 'users' && '👥 Customers'}
              {activeTab === 'settings' && '⚙️ Settings'}
            </h1>
          </div>
          <div className="topbar-right">
            {/* Enhanced Search Box */}
            <div className={`search-box ${searchQuery ? 'has-value' : ''}`}>
              <Search size={18} className="search-icon" />
              <input 
                type="text" 
                placeholder={`Search ${activeTab}...`}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              {searchQuery && (
                <>
                  {getSearchResultCount() !== null && (
                    <span className="search-count">{getSearchResultCount()} found</span>
                  )}
                  <button className="search-clear" onClick={clearSearch} title="Clear search">
                    <XCircle size={16} />
                  </button>
                </>
              )}
            </div>
            
            {/* Notifications Bell */}
            <div className="notification-wrapper">
              <button 
                className={`topbar-btn notification-btn ${showNotifications ? 'active' : ''}`} 
                title="Notifications"
                onClick={() => setShowNotifications(!showNotifications)}
              >
                <Bell size={20} />
                {notificationCount > 0 && (
                  <span className="notification-badge">{notificationCount}</span>
                )}
              </button>
              
              {/* Notifications Dropdown */}
              {showNotifications && (
                <div className="notifications-dropdown">
                  <div className="notifications-header">
                    <h3>🔔 Notifications</h3>
                    <span className="notification-count-label">{currentNotifications.length} alerts</span>
                  </div>
                  <div className="notifications-list">
                    {currentNotifications.length === 0 ? (
                      <div className="notification-empty">
                        <span className="empty-icon">✨</span>
                        <span>All caught up!</span>
                        <span className="empty-sub">No pending notifications</span>
                      </div>
                    ) : (
                      currentNotifications.map((notif) => (
                        <div 
                          key={notif.id} 
                          className={`notification-item ${notif.type}`}
                          onClick={notif.action}
                        >
                          <span className="notif-icon">{notif.icon}</span>
                          <div className="notif-content">
                            <span className="notif-title">{notif.title}</span>
                            <span className="notif-message">{notif.message}</span>
                          </div>
                          <ArrowUpRight size={14} className="notif-arrow" />
                        </div>
                      ))
                    )}
                  </div>
                  <div className="notifications-footer">
                    <button onClick={() => { setActiveTab('dashboard'); setShowNotifications(false); }}>
                      View Dashboard
                    </button>
                  </div>
                </div>
              )}
            </div>
            
            <button className="topbar-btn refresh" onClick={refreshAll} title="Refresh Data">
              <RefreshCw size={20} />
            </button>
          </div>
        </header>
        
        {/* Click outside to close notifications */}
        {showNotifications && (
          <div className="notification-overlay" onClick={() => setShowNotifications(false)}></div>
        )}

        {/* Content Area */}
        <div className="admin-content">
          {error && (
            <div className="alert alert-error">
              <AlertCircle size={18} />
              <span>{error}</span>
              <button onClick={() => setError(null)}>×</button>
            </div>
          )}
          
          {/* ==================== DASHBOARD ==================== */}
          {activeTab === 'dashboard' && (
            <div className="dashboard">
              {/* Live Status Header */}
              <div className="dashboard-header">
                <div className="live-status">
                  <span className={`live-indicator ${isLive ? 'active' : ''}`}></span>
                  <span className="live-text">{isLive ? 'LIVE' : 'PAUSED'}</span>
                  <button 
                    className={`live-toggle ${isLive ? 'active' : ''}`}
                    onClick={() => setIsLive(!isLive)}
                    title={isLive ? 'Pause auto-refresh' : 'Enable auto-refresh'}
                  >
                    {isLive ? '⏸' : '▶'}
                  </button>
                </div>
                <div className="last-updated">
                  Last updated: {lastUpdated.toLocaleTimeString('en-IN')}
                </div>
              </div>

              {/* Main Stats Row */}
              <div className="stats-grid">
                <div className="stat-card gradient-purple">
                  <div className="stat-icon"><DollarSign size={28} /></div>
                  <div className="stat-content">
                    <span className="stat-label">Total Revenue</span>
                    <span className="stat-value">{formatINR(analytics.totalRevenue)}</span>
                    <span className="stat-change positive"><ArrowUpRight size={16} /> {orders.length} orders</span>
                  </div>
                </div>
                <div className="stat-card gradient-blue">
                  <div className="stat-icon"><ShoppingCart size={28} /></div>
                  <div className="stat-content">
                    <span className="stat-label">Total Orders</span>
                    <span className="stat-value">{orders.length}</span>
                    <span className="stat-change warning">{analytics.pendingOrders} pending</span>
                  </div>
                </div>
                <div className="stat-card gradient-green">
                  <div className="stat-icon"><Package size={28} /></div>
                  <div className="stat-content">
                    <span className="stat-label">Total Stock</span>
                    <span className="stat-value">{analytics.totalStock.toLocaleString()} units</span>
                    <span className="stat-change neutral">{products.length} products</span>
                  </div>
                </div>
                <div className="stat-card gradient-orange">
                  <div className="stat-icon"><Users size={28} /></div>
                  <div className="stat-content">
                    <span className="stat-label">Total Customers</span>
                    <span className="stat-value">{analytics.totalCustomers}</span>
                    <span className="stat-change positive">{analytics.activeCustomers} active</span>
                  </div>
                </div>
              </div>

              {/* Secondary Stats Row */}
              <div className="stats-grid secondary">
                <div className="stat-card-mini">
                  <div className="mini-icon success">✓</div>
                  <div className="mini-content">
                    <span className="mini-value">{analytics.deliveredOrders}</span>
                    <span className="mini-label">Delivered</span>
                  </div>
                </div>
                <div className="stat-card-mini">
                  <div className="mini-icon info">📦</div>
                  <div className="mini-content">
                    <span className="mini-value">{analytics.shippedOrders}</span>
                    <span className="mini-label">Shipped</span>
                  </div>
                </div>
                <div className="stat-card-mini">
                  <div className="mini-icon warning">⏳</div>
                  <div className="mini-content">
                    <span className="mini-value">{analytics.processingOrders}</span>
                    <span className="mini-label">Processing</span>
                  </div>
                </div>
                <div className="stat-card-mini">
                  <div className="mini-icon danger">⚠️</div>
                  <div className="mini-content">
                    <span className="mini-value">{analytics.lowStockProducts.length}</span>
                    <span className="mini-label">Low Stock</span>
                  </div>
                </div>
                <div className="stat-card-mini">
                  <div className="mini-icon purple">💰</div>
                  <div className="mini-content">
                    <span className="mini-value">{formatINR(analytics.avgOrderValue)}</span>
                    <span className="mini-label">Avg Order</span>
                  </div>
                </div>
                <div className="stat-card-mini">
                  <div className="mini-icon blue">📊</div>
                  <div className="mini-content">
                    <span className="mini-value">{formatINR(analytics.inventoryValue)}</span>
                    <span className="mini-label">Inventory Value</span>
                  </div>
                </div>
              </div>

              <div className="dashboard-grid">
                <div className="card">
                  <div className="card-header">
                    <h3>📋 Recent Orders</h3>
                    <button className="btn-link" onClick={() => setActiveTab('orders')}>View All</button>
                  </div>
                  <div className="card-body">
                    {orders.slice(0, 5).length > 0 ? (
                      <div className="recent-orders-list">
                        {orders.slice(0, 5).map(order => (
                          <div key={order.id} className="recent-order-item" onClick={() => openOrderModal(order)}>
                            <div className="order-info">
                              <span className="order-id">#{order.id}</span>
                              <span className="order-customer">{order.customerName || 'Customer'}</span>
                            </div>
                            <div className="order-meta">
                              <span className={`status-badge ${getStatusColor(order.status)}`}>
                                {order.status || 'Pending'}
                              </span>
                              <span className="order-amount">{formatINR(order.totalAmount)}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state">
                        <ShoppingCart size={48} />
                        <p>No orders yet</p>
                      </div>
                    )}
                  </div>
                </div>

                <div className="card">
                  <div className="card-header">
                    <h3>⚠️ Low Stock Alert</h3>
                    <button className="btn-link" onClick={() => setActiveTab('products')}>View All</button>
                  </div>
                  <div className="card-body">
                    {analytics.lowStockProducts.length > 0 ? (
                      <div className="low-stock-list">
                        {analytics.lowStockProducts.slice(0, 5).map(product => (
                          <div key={product.id} className="low-stock-item">
                            <div className="product-info">
                              <span className="product-name">{product.name}</span>
                              <span className="product-brand">{product.brand}</span>
                            </div>
                            <span className={`stock-count ${product.stock <= 5 ? 'critical' : 'warning'}`}>
                              {product.stock} left
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state">
                        <Check size={48} />
                        <p>All products well stocked!</p>
                      </div>
                    )}
                  </div>
                </div>

                <div className="card">
                  <div className="card-header">
                    <h3>⚡ Quick Actions</h3>
                  </div>
                  <div className="card-body">
                    <div className="quick-actions">
                      <button className="quick-action-btn" onClick={openAddProductModal}>
                        <Plus size={20} />
                        <span>Add Product</span>
                      </button>
                      <button className="quick-action-btn" onClick={() => setActiveTab('orders')}>
                        <Eye size={20} />
                        <span>View Orders</span>
                      </button>
                      <button className="quick-action-btn" onClick={() => setActiveTab('users')}>
                        <Users size={20} />
                        <span>Manage Users</span>
                      </button>
                      <button className="quick-action-btn" onClick={() => setActiveTab('settings')}>
                        <Settings size={20} />
                        <span>Settings</span>
                      </button>
                    </div>
                  </div>
                </div>

                <div className="card">
                  <div className="card-header">
                    <h3>🏆 Top Products</h3>
                    <button className="btn-link" onClick={() => setActiveTab('products')}>View All</button>
                  </div>
                  <div className="card-body">
                    {products.slice(0, 5).length > 0 ? (
                      <div className="top-products-list">
                        {products.slice(0, 5).map((product, idx) => (
                          <div key={product.id} className="top-product-item" onClick={() => openEditProductModal(product)}>
                            <span className="rank">#{idx + 1}</span>
                            <div className="product-info">
                              <span className="product-name">{product.name}</span>
                              <span className="product-category">{product.category}</span>
                            </div>
                            <span className="product-price">{formatINR(product.price)}</span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state">
                        <Package size={48} />
                        <p>No products yet</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ==================== PRODUCTS ==================== */}
          {activeTab === 'products' && (
            <div className="section">
              <div className="section-header">
                <div className="section-title">
                  <h2>Product Catalog</h2>
                  <span className="subtitle">{filteredProducts.length} products found</span>
                </div>
                <button className="btn btn-primary" onClick={openAddProductModal}>
                  <Plus size={18} />
                  Add New Product
                </button>
              </div>

              {loading ? (
                <div className="loading-state">
                  <div className="spinner"></div>
                  <p>Loading products...</p>
                </div>
              ) : filteredProducts.length > 0 ? (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Rating</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredProducts.map(product => (
                        <tr key={product.id}>
                          <td>
                            <div className="product-cell">
                              <div className="product-thumb">
                                {product.imageUrl ? (
                                  <img src={product.imageUrl} alt={product.name} />
                                ) : (
                                  <Package size={24} />
                                )}
                              </div>
                              <div className="product-details">
                                <span className="product-name">{product.name}</span>
                                <span className="product-id">{product.brand || `ID: ${product.id}`}</span>
                              </div>
                            </div>
                          </td>
                          <td><span className="category-badge">{product.category}</span></td>
                          <td className="price">{formatINR(product.price)}</td>
                          <td>
                            <span className={`stock-badge ${product.stock < 10 ? 'low' : 'ok'}`}>
                              {product.stock} units
                            </span>
                          </td>
                          <td>
                            <div className="rating">
                              <span className="star">⭐</span>
                              <span>{product.rating?.toFixed(1) || 'N/A'}</span>
                            </div>
                          </td>
                          <td>
                            <div className="action-buttons">
                              <button className="action-btn edit" title="Edit" onClick={() => openEditProductModal(product)}>
                                <Edit size={16} />
                              </button>
                              <button className="action-btn delete" title="Delete" onClick={() => confirmDeleteProduct(product)}>
                                <Trash2 size={16} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state large">
                  <Package size={64} />
                  <h3>No Products Found</h3>
                  <p>Start by adding your first product</p>
                  <button className="btn btn-primary" onClick={openAddProductModal}>
                    <Plus size={18} /> Add Product
                  </button>
                </div>
              )}
            </div>
          )}

          {/* ==================== ORDERS ==================== */}
          {activeTab === 'orders' && (
            <div className="section">
              <div className="section-header">
                <div className="section-title">
                  <h2>Order Management</h2>
                  <span className="subtitle">{filteredOrders.length} total orders</span>
                </div>
              </div>

              {loading ? (
                <div className="loading-state">
                  <div className="spinner"></div>
                  <p>Loading orders...</p>
                </div>
              ) : filteredOrders.length > 0 ? (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Date</th>
                        <th>Total</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredOrders.map(order => (
                        <tr key={order.id}>
                          <td className="order-id">#{order.id}</td>
                          <td>
                            <div className="customer-cell">
                              <div className="customer-avatar">
                                {(order.customerName || 'C').charAt(0)}
                              </div>
                              <span>{order.customerName || order.user?.firstName || 'Customer'}</span>
                            </div>
                          </td>
                          <td className="date">
                            <Calendar size={14} />
                            {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : 'N/A'}
                          </td>
                          <td className="price">{formatINR(order.totalAmount)}</td>
                          <td>
                            <select 
                              className={`status-select ${getStatusColor(order.status)}`}
                              value={order.status || 'PLACED'}
                              onChange={(e) => handleUpdateOrderStatus(order.id, e.target.value)}
                            >
                              {orderStatuses.map(status => (
                                <option key={status} value={status}>{status}</option>
                              ))}
                            </select>
                          </td>
                          <td>
                            <div className="action-buttons">
                              <button className="action-btn view" title="View Details" onClick={() => openOrderModal(order)}>
                                <Eye size={16} />
                              </button>
                              <button className="action-btn invoice" title="Download Invoice" onClick={() => generateInvoicePDF(order)}>
                                <FileText size={16} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state large">
                  <ShoppingCart size={64} />
                  <h3>No Orders Yet</h3>
                  <p>Orders will appear here when customers make purchases</p>
                </div>
              )}
            </div>
          )}

          {/* ==================== USERS ==================== */}
          {activeTab === 'users' && (
            <div className="section">
              <div className="section-header">
                <div className="section-title">
                  <h2>Customer Management</h2>
                  <span className="subtitle">{filteredUsers.length} registered users</span>
                </div>
              </div>

              {loading ? (
                <div className="loading-state">
                  <div className="spinner"></div>
                  <p>Loading users...</p>
                </div>
              ) : filteredUsers.length > 0 ? (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>User</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredUsers.map(u => (
                        <tr key={u.id}>
                          <td>
                            <div className="user-cell">
                              <div className="user-avatar-sm">
                                {(u.firstName || 'U').charAt(0)}
                              </div>
                              <span>{u.firstName} {u.lastName}</span>
                            </div>
                          </td>
                          <td className="email">{u.email}</td>
                          <td>
                            <span className={`role-badge ${u.role?.toLowerCase()}`}>
                              {u.role}
                            </span>
                          </td>
                          <td>
                            <button 
                              className={`status-toggle ${u.active ? 'active' : 'inactive'}`}
                              onClick={() => handleToggleUserStatus(u.id, u.active)}
                            >
                              {u.active ? <Check size={14} /> : <XCircle size={14} />}
                              {u.active ? 'Active' : 'Inactive'}
                            </button>
                          </td>
                          <td>
                            <div className="action-buttons">
                              <button className="action-btn view" title="View Profile" onClick={() => openUserModal(u)}>
                                <Eye size={16} />
                              </button>
                              <button className="action-btn edit" title="Edit User" onClick={() => openUserModal(u)}>
                                <Edit size={16} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="empty-state large">
                  <Users size={64} />
                  <h3>No Users Found</h3>
                  <p>Users will appear here when they register</p>
                </div>
              )}
            </div>
          )}

          {/* ==================== SETTINGS ==================== */}
          {activeTab === 'settings' && (
            <div className="section settings-section">
              <div className="section-header">
                <div className="section-title">
                  <h2>Store Settings</h2>
                  <span className="subtitle">Manage your store configuration</span>
                </div>
              </div>

              <div className="settings-grid">
                <div className="settings-card">
                  <h3>🏪 General Settings</h3>
                  <div className="form-group">
                    <label>Store Name</label>
                    <input 
                      type="text" 
                      className="form-input"
                      value={settingsForm.storeName}
                      onChange={(e) => setSettingsForm({...settingsForm, storeName: e.target.value})}
                    />
                  </div>
                  <div className="form-group">
                    <label>Store Email</label>
                    <input 
                      type="email" 
                      className="form-input"
                      value={settingsForm.storeEmail}
                      onChange={(e) => setSettingsForm({...settingsForm, storeEmail: e.target.value})}
                    />
                  </div>
                  <div className="form-group">
                    <label>Support Phone</label>
                    <input 
                      type="tel" 
                      className="form-input"
                      value={settingsForm.supportPhone}
                      onChange={(e) => setSettingsForm({...settingsForm, supportPhone: e.target.value})}
                    />
                  </div>
                </div>

                <div className="settings-card">
                  <h3>💳 Payment Settings</h3>
                  <div className="form-group">
                    <label>Currency</label>
                    <select 
                      className="form-input"
                      value={settingsForm.currency}
                      onChange={(e) => setSettingsForm({...settingsForm, currency: e.target.value})}
                    >
                      <option value="USD">USD - US Dollar</option>
                      <option value="EUR">EUR - Euro</option>
                      <option value="GBP">GBP - British Pound</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Tax Rate (%)</label>
                    <input 
                      type="number" 
                      className="form-input"
                      value={settingsForm.taxRate}
                      onChange={(e) => setSettingsForm({...settingsForm, taxRate: e.target.value})}
                    />
                  </div>
                </div>

                <div className="settings-card">
                  <h3>📦 Shipping Settings</h3>
                  <div className="form-group">
                    <label>Free Shipping Threshold ($)</label>
                    <input 
                      type="number" 
                      className="form-input"
                      value={settingsForm.freeShippingThreshold}
                      onChange={(e) => setSettingsForm({...settingsForm, freeShippingThreshold: e.target.value})}
                    />
                  </div>
                  <div className="form-group">
                    <label>Default Shipping Cost ($)</label>
                    <input 
                      type="number" 
                      step="0.01"
                      className="form-input"
                      value={settingsForm.defaultShippingCost}
                      onChange={(e) => setSettingsForm({...settingsForm, defaultShippingCost: e.target.value})}
                    />
                  </div>
                </div>
              </div>

              <div className="settings-actions">
                <button className="btn btn-secondary" onClick={() => {
                  const savedSettings = localStorage.getItem('storeSettings');
                  if (savedSettings) setSettingsForm(JSON.parse(savedSettings));
                }}>
                  Cancel
                </button>
                <button className="btn btn-primary" onClick={handleSaveSettings}>
                  <Save size={18} />
                  Save Changes
                </button>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* ==================== PRODUCT MODAL ==================== */}
      {showProductModal && (
        <div className="modal-overlay" onClick={() => setShowProductModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{modalMode === 'add' ? '➕ Add New Product' : '✏️ Edit Product'}</h2>
              <button className="modal-close" onClick={() => setShowProductModal(false)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleProductSubmit} className="modal-form">
              <div className="modal-body">
                <div className="form-row">
                  <div className="form-group">
                    <label>Product Name *</label>
                    <input 
                      type="text" 
                      className="form-input"
                      value={productForm.name}
                      onChange={(e) => setProductForm({...productForm, name: e.target.value})}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Brand</label>
                    <input 
                      type="text" 
                      className="form-input"
                      value={productForm.brand}
                      onChange={(e) => setProductForm({...productForm, brand: e.target.value})}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Description</label>
                  <textarea 
                    className="form-input"
                    rows={3}
                    value={productForm.description}
                    onChange={(e) => setProductForm({...productForm, description: e.target.value})}
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Price ($) *</label>
                    <input 
                      type="number" 
                      step="0.01"
                      min="0"
                      className="form-input"
                      value={productForm.price}
                      onChange={(e) => setProductForm({...productForm, price: e.target.value})}
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>Stock *</label>
                    <input 
                      type="number" 
                      min="0"
                      className="form-input"
                      value={productForm.stock}
                      onChange={(e) => setProductForm({...productForm, stock: e.target.value})}
                      required
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Category *</label>
                    <select 
                      className="form-input"
                      value={productForm.category}
                      onChange={(e) => setProductForm({...productForm, category: e.target.value})}
                      required
                    >
                      {categories.map(cat => (
                        <option key={cat} value={cat}>{cat}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Size</label>
                    <select 
                      className="form-input"
                      value={productForm.size}
                      onChange={(e) => setProductForm({...productForm, size: e.target.value})}
                    >
                      <option value="30ml">30ml</option>
                      <option value="50ml">50ml</option>
                      <option value="75ml">75ml</option>
                      <option value="100ml">100ml</option>
                      <option value="150ml">150ml</option>
                      <option value="200ml">200ml</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Type</label>
                    <select 
                      className="form-input"
                      value={productForm.type}
                      onChange={(e) => setProductForm({...productForm, type: e.target.value})}
                    >
                      <option value="Eau de Parfum">Eau de Parfum</option>
                      <option value="Eau de Toilette">Eau de Toilette</option>
                      <option value="Eau de Cologne">Eau de Cologne</option>
                      <option value="Parfum">Parfum</option>
                      <option value="Eau Fraiche">Eau Fraiche</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Status</label>
                    <select 
                      className="form-input"
                      value={productForm.active ? 'active' : 'inactive'}
                      onChange={(e) => setProductForm({...productForm, active: e.target.value === 'active'})}
                    >
                      <option value="active">✓ Active</option>
                      <option value="inactive">✗ Inactive</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>Product Image</label>
                  
                  {/* Upload Method Toggle */}
                  <div className="upload-toggle">
                    <button 
                      type="button"
                      className={`toggle-btn ${uploadMethod === 'upload' ? 'active' : ''}`}
                      onClick={() => setUploadMethod('upload')}
                    >
                      <Upload size={16} />
                      Upload Image
                    </button>
                    <button 
                      type="button"
                      className={`toggle-btn ${uploadMethod === 'url' ? 'active' : ''}`}
                      onClick={() => setUploadMethod('url')}
                    >
                      <ImageIcon size={16} />
                      Image URL
                    </button>
                  </div>
                  
                  {uploadMethod === 'upload' ? (
                    <div className="image-upload-area">
                      <input 
                        type="file" 
                        ref={fileInputRef}
                        accept="image/*"
                        onChange={handleImageUpload}
                        className="file-input-hidden"
                        id="product-image-upload"
                      />
                      {!imagePreview && !productForm.imageUrl ? (
                        <label htmlFor="product-image-upload" className="upload-dropzone">
                          <Upload size={40} />
                          <span className="upload-text">Click to upload image</span>
                          <span className="upload-hint">PNG, JPG, WEBP up to 5MB</span>
                        </label>
                      ) : (
                        <div className="uploaded-image-preview">
                          <img 
                            src={imagePreview || productForm.imageUrl} 
                            alt="Preview" 
                          />
                          <div className="image-overlay">
                            <button type="button" className="remove-image-btn" onClick={removeImage}>
                              <XCircle size={20} />
                              Remove
                            </button>
                            <label htmlFor="product-image-upload" className="change-image-btn">
                              <RefreshCw size={20} />
                              Change
                            </label>
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <>
                      <input 
                        type="text" 
                        className="form-input"
                        placeholder="https://example.com/image.jpg"
                        value={productForm.imageUrl}
                        onChange={(e) => setProductForm({...productForm, imageUrl: e.target.value})}
                      />
                      {productForm.imageUrl && (
                        <div className="image-preview">
                          <img 
                            src={productForm.imageUrl} 
                            alt="Preview" 
                            onError={(e) => e.target.style.display = 'none'}
                            onLoad={(e) => e.target.style.display = 'block'}
                          />
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowProductModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Saving...' : (modalMode === 'add' ? 'Create Product' : 'Update Product')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==================== ORDER MODAL ==================== */}
      {showOrderModal && selectedItem && (
        <div className="modal-overlay" onClick={() => setShowOrderModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>📦 Order #{selectedItem.id}</h2>
              <button className="modal-close" onClick={() => setShowOrderModal(false)}>
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="order-details">
                <div className="detail-row">
                  <span className="label">Customer:</span>
                  <span className="value">{selectedItem.customerName || selectedItem.user?.firstName || 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Email:</span>
                  <span className="value">{selectedItem.customerEmail || selectedItem.user?.email || 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Date:</span>
                  <span className="value">{selectedItem.createdAt ? new Date(selectedItem.createdAt).toLocaleString() : 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Total:</span>
                  <span className="value price">{formatINR(selectedItem.totalAmount)}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Status:</span>
                  <select 
                    className={`status-select ${getStatusColor(selectedItem.status)}`}
                    value={selectedItem.status || 'PENDING'}
                    onChange={(e) => handleUpdateOrderStatus(selectedItem.id, e.target.value)}
                  >
                    {orderStatuses.map(status => (
                      <option key={status} value={status}>{status}</option>
                    ))}
                  </select>
                </div>
                {selectedItem.shippingAddress && (
                  <div className="detail-row">
                    <span className="label">Shipping:</span>
                    <span className="value">{selectedItem.shippingAddress}</span>
                  </div>
                )}
              </div>

              {selectedItem.items && selectedItem.items.length > 0 && (
                <div className="order-items">
                  <h4>Order Items</h4>
                  <table className="mini-table">
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th>Qty</th>
                        <th>Price</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedItem.items.map((item, idx) => (
                        <tr key={idx}>
                          <td>{item.productName || item.product?.name || 'Product'}</td>
                          <td>{item.quantity}</td>
                          <td>{formatINR(item.price)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowOrderModal(false)}>
                Close
              </button>
              <button className="invoice-download-btn" onClick={() => generateInvoicePDF(selectedItem)}>
                <Download size={18} />
                Download Invoice
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================== USER MODAL ==================== */}
      {showUserModal && selectedItem && (
        <div className="modal-overlay" onClick={() => setShowUserModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>👤 User Details</h2>
              <button className="modal-close" onClick={() => setShowUserModal(false)}>
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="user-profile-card">
                <div className="profile-avatar">
                  {(selectedItem.firstName || 'U').charAt(0)}
                </div>
                <div className="profile-info">
                  <h3>{selectedItem.firstName} {selectedItem.lastName}</h3>
                  <p>{selectedItem.email}</p>
                </div>
              </div>

              <div className="order-details">
                <div className="detail-row">
                  <span className="label">User ID:</span>
                  <span className="value">#{selectedItem.id}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Phone:</span>
                  <span className="value">{selectedItem.phoneNumber || 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Address:</span>
                  <span className="value">{selectedItem.address || 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="label">City:</span>
                  <span className="value">{selectedItem.city || 'N/A'}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Role:</span>
                  <select 
                    className="form-input"
                    value={selectedItem.role || 'CUSTOMER'}
                    onChange={(e) => handleChangeUserRole(selectedItem.id, e.target.value)}
                  >
                    <option value="CUSTOMER">CUSTOMER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
                <div className="detail-row">
                  <span className="label">Status:</span>
                  <button 
                    className={`status-toggle ${selectedItem.active ? 'active' : 'inactive'}`}
                    onClick={() => {
                      handleToggleUserStatus(selectedItem.id, selectedItem.active);
                      setSelectedItem({...selectedItem, active: !selectedItem.active});
                    }}
                  >
                    {selectedItem.active ? <Check size={14} /> : <XCircle size={14} />}
                    {selectedItem.active ? 'Active' : 'Inactive'}
                  </button>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowUserModal(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================== DELETE CONFIRMATION ==================== */}
      {showDeleteConfirm && (
        <div className="confirm-modal" onClick={() => setShowDeleteConfirm(false)}>
          <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="icon-danger">
              <AlertCircle size={32} />
            </div>
            <h3>Delete Product?</h3>
            <p>
              Are you sure you want to delete <strong>"{selectedItem?.name}"</strong>? 
              This action cannot be undone.
            </p>
            <div className="confirm-actions">
              <button className="btn-cancel" onClick={() => setShowDeleteConfirm(false)}>
                Cancel
              </button>
              <button className="btn-danger" onClick={handleDeleteProduct} disabled={loading}>
                {loading ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
