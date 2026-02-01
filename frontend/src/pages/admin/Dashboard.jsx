import { useState, useEffect } from 'react';
import { Routes, Route, Link, useLocation } from 'react-router-dom';
import api from '../../api/axios';
import Overview from './Overview';
import ProductManagement from './ProductManagement';
import OrderManagement from './OrderManagement';
import UserManagement from './UserManagement';

export default function Dashboard() {
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [adminProfile, setAdminProfile] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAdminProfile = async () => {
      try {
        const response = await api.get('/admin/profile');
        setAdminProfile(response.data);
        setLoading(false);
      } catch (err) {
        console.error('Error fetching admin profile:', err);
        setError('Failed to load admin profile');
        setLoading(false);
      }
    };

    fetchAdminProfile();
  }, []);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <p className="text-center text-gray-600">Loading admin dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <p className="text-center text-red-600">{error}</p>
      </div>
    );
  }

  const navItems = [
    { name: 'Overview', path: '', icon: '📊' },
    { name: 'Products', path: 'products', icon: '📦' },
    { name: 'Orders', path: 'orders', icon: '🛒' },
    { name: 'Users', path: 'users', icon: '👥' },
  ];

  const isActive = (path) => {
    const currentPath = location.pathname.split('/admin')[1] || '';
    const currentRoute = currentPath.replace(/^\//, '');
    return currentRoute.startsWith(path);
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
            {adminProfile && (
              <div className="text-right">
                <p className="text-sm text-gray-600">Welcome back</p>
                <p className="font-semibold text-gray-900">
                  {adminProfile.firstName} {adminProfile.lastName}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex gap-8">
          {/* Sidebar Navigation */}
          <nav className="w-48 flex-shrink-0">
            <div className="bg-white rounded-lg shadow">
              <div className="p-4">
                <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-4">
                  Management
                </h3>
                <ul className="space-y-2">
                  {navItems.map((item) => (
                    <li key={item.path}>
                      <Link
                        to={`/admin${item.path ? '/' + item.path : ''}`}
                        className={`flex items-center gap-3 px-4 py-2 rounded-lg transition-colors ${
                          isActive(item.path)
                            ? 'bg-blue-100 text-blue-700 font-semibold'
                            : 'text-gray-700 hover:bg-gray-100'
                        }`}
                      >
                        <span className="text-lg">{item.icon}</span>
                        {item.name}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </nav>

          {/* Main Content */}
          <div className="flex-1">
            <Routes>
              <Route path="" element={<Overview />} />
              <Route path="products/*" element={<ProductManagement />} />
              <Route path="orders/*" element={<OrderManagement />} />
              <Route path="users/*" element={<UserManagement />} />
            </Routes>
          </div>
        </div>
      </div>
    </div>
  );
}
