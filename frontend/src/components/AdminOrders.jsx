import React, { useState, useEffect } from 'react';
import { Eye, CheckCircle, Clock, X as XIcon } from 'lucide-react';
import { toast } from 'react-toastify';
import '../styles/AdminOrders.css';

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('All');

  useEffect(() => {
    // Demo mode: Use mock orders data
    setOrders([
      {
        id: 'ORD-001',
        customerName: 'John Doe',
        email: 'john@example.com',
        status: 'Delivered',
        total: 299.99,
        date: '2024-01-15',
        items: 3
      },
      {
        id: 'ORD-002',
        customerName: 'Jane Smith',
        email: 'jane@example.com',
        status: 'Processing',
        total: 189.50,
        date: '2024-01-20',
        items: 2
      }
    ]);
    setLoading(false);
  }, []);

  const fetchOrders = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/admin/orders?size=50', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (response.ok) {
        const data = await response.json();
        setOrders(data.content || []);
      } else {
        // Gracefully handle auth errors - show empty list
        console.warn('Failed to load admin orders, showing empty list');
        setOrders([]);
      }
    } catch (error) {
      console.error('Error fetching orders:', error);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  const filteredOrders = filter === 'All' 
    ? orders 
    : orders.filter(o => {
        const status = o.status?.toUpperCase() || '';
        return status === filter.toUpperCase();
      });

  const getStatusIcon = (status) => {
    const statusUpper = status?.toUpperCase() || '';
    if (['PENDING', 'PROCESSING'].includes(statusUpper)) return <Clock size={16} />;
    if (['SHIPPED', 'DELIVERED'].includes(statusUpper)) return <CheckCircle size={16} />;
    if (statusUpper === 'CANCELLED') return <XIcon size={16} />;
    return null;
  };

  const getStatusDisplay = (status) => {
    const statusMap = {
      'PENDING': 'Pending',
      'PROCESSING': 'Processing',
      'SHIPPED': 'Shipped',
      'DELIVERED': 'Delivered',
      'CANCELLED': 'Cancelled',
      'PAYMENT_PENDING': 'Payment Pending',
      'PAYMENT_CONFIRMED': 'Payment Confirmed'
    };
    return statusMap[status?.toUpperCase()] || status || 'Unknown';
  };

  return (
    <div className="orders-container">
      <div className="orders-header">
        <h2>Orders Management</h2>
        <div className="order-filters">
          {['All', 'Pending', 'Processing', 'Shipped', 'Delivered'].map(status => (
            <button
              key={status}
              className={`filter-btn ${filter === status ? 'active' : ''}`}
              onClick={() => setFilter(status)}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      <div className="orders-table-wrapper">
        <table className="orders-table">
          <thead>
            <tr>
              <th>Order Number</th>
              <th>Customer</th>
              <th>Items</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Date</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7" className="loading">Loading...</td></tr>
            ) : filteredOrders.length === 0 ? (
              <tr><td colSpan="7" className="empty">No orders found</td></tr>
            ) : (
              filteredOrders.map(order => (
                <tr key={order.id}>
                  <td><strong>{order.orderNumber || `#ORD-${order.id}`}</strong></td>
                  <td>{order.user?.firstName} {order.user?.lastName || 'N/A'}</td>
                  <td>{order.items?.length || 0}</td>
                  <td className="amount">₹{order.totalAmount?.toFixed(2) || '0.00'}</td>
                  <td>
                    <span className={`status-badge ${order.status?.toLowerCase()}`}>
                      {getStatusIcon(order.status)}
                      {getStatusDisplay(order.status)}
                    </span>
                  </td>
                  <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                  <td>
                    <button className="btn-view" title="View Details">
                      <Eye size={16} /> View
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
