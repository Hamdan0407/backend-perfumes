import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import api from '../api/axios';
import { useCartStore } from '../store/cartStore';

/**
 * Razorpay payment form component
 * Integrates Razorpay payment gateway for secure checkout
 * Supports demo mode for testing without real Razorpay credentials
 */
function RazorpayPaymentForm({ razorpayOrderResponse, onPaymentSuccess }) {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { clearCart } = useCartStore();
  
  // Check if we're in demo mode
  const isDemoMode = razorpayOrderResponse.razorpayKeyId === 'rzp_test_demo_mode';

  useEffect(() => {
    // Only load Razorpay script if not in demo mode
    if (!isDemoMode) {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.async = true;
      document.body.appendChild(script);

      return () => {
        document.body.removeChild(script);
      };
    }
  }, [isDemoMode]);

  // Demo mode payment handler - simulates successful payment
  const handleDemoPayment = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Simulate payment processing
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // In demo mode, directly confirm the order
      await api.post('/orders/verify-payment', {
        razorpayOrderId: razorpayOrderResponse.razorpayOrderId,
        razorpayPaymentId: 'demo_payment_' + Date.now(),
        razorpaySignature: 'demo_signature',
      });

      toast.success('Demo payment successful! Your order is confirmed.');
      clearCart();
      
      // Navigate to order details page
      setTimeout(() => {
        navigate(`/orders/${razorpayOrderResponse.orderId}`);
      }, 1500);
      
    } catch (error) {
      console.error('Demo payment failed:', error);
      toast.error('Demo payment failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const options = {
        key: razorpayOrderResponse.razorpayKeyId, // Razorpay Public Key
        amount: razorpayOrderResponse.amount, // Amount in paise
        currency: razorpayOrderResponse.currency,
        order_id: razorpayOrderResponse.razorpayOrderId, // Razorpay Order ID
        name: 'Perfume Shop',
        description: `Order #${razorpayOrderResponse.orderNumber}`,
        
        // Customer details
        prefill: {
          name: razorpayOrderResponse.customerName || '',
          email: razorpayOrderResponse.customerEmail || '',
          contact: razorpayOrderResponse.customerPhone || '',
        },
        
        // Callback handlers
        handler: async function (response) {
          // Payment successful, now verify signature on backend
          try {
            const verificationResult = await api.post('/orders/verify-payment', {
              razorpayOrderId: options.order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });

            toast.success('Payment successful! Your order is confirmed.');
            
            // Navigate to order details page
            setTimeout(() => {
              navigate(`/orders/${razorpayOrderResponse.orderId}`);
            }, 1500);
            
          } catch (error) {
            console.error('Payment verification failed:', error);
            toast.error('Payment verification failed. Please contact support.');
          }
        },
        
        // Error handler
        modal: {
          ondismiss: function () {
            setLoading(false);
            toast.error('Payment cancelled. Order remains pending.');
          }
        },
      };

      // Open Razorpay checkout modal
      if (window.Razorpay) {
        const razorpay = new window.Razorpay(options);
        razorpay.open();
      } else {
        throw new Error('Razorpay SDK not loaded');
      }

    } catch (error) {
      console.error('Payment initiation error:', error);
      toast.error('Failed to initiate payment. Please try again.');
      setLoading(false);
    }
  };

  return (
    <form onSubmit={isDemoMode ? handleDemoPayment : handlePayment} className="space-y-4">
      {isDemoMode && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-4">
          <p className="text-yellow-800 text-sm font-medium">
            🧪 Demo Mode - No real payment will be processed
          </p>
          <p className="text-yellow-700 text-xs mt-1">
            Razorpay is not configured. Click the button below to simulate a successful payment.
          </p>
        </div>
      )}
      
      <div className="bg-gray-50 p-4 rounded-lg">
        <div className="flex justify-between items-center mb-2">
          <span className="text-gray-600">Amount:</span>
          <span className="font-semibold text-lg">₹{(razorpayOrderResponse.amount / 100).toFixed(2)}</span>
        </div>
        <div className="flex justify-between items-center">
          <span className="text-gray-600">Order ID:</span>
          <span className="font-mono text-sm">{razorpayOrderResponse.razorpayOrderId}</span>
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full btn-primary disabled:opacity-50"
      >
        {loading ? 'Processing...' : (isDemoMode ? 'Complete Demo Payment' : 'Pay with Razorpay')}
      </button>
      
      <p className="text-sm text-gray-500 text-center">
        {isDemoMode ? 'Demo mode - no actual payment required.' : 'Secured by Razorpay. Your payment information is encrypted.'}
      </p>
    </form>
  );
}

/**
 * Main Checkout Page Component
 * Handles shipping information and payment flow
 */
export default function Checkout() {
  const navigate = useNavigate();
  const { cart, setCart } = useCartStore();
  const [loading, setLoading] = useState(false);
  const [cartLoading, setCartLoading] = useState(true);
  const [razorpayOrderResponse, setRazorpayOrderResponse] = useState(null);
  const [shippingInfo, setShippingInfo] = useState({
    shippingAddress: '',
    shippingCity: '',
    shippingCountry: '',
    shippingZipCode: '',
    shippingPhone: ''
  });

  // Fetch cart on mount to ensure we have the latest data
  useEffect(() => {
    const fetchCart = async () => {
      try {
        const { data } = await api.get('/cart');
        setCart(data);
      } catch (error) {
        console.error('Failed to fetch cart:', error);
      } finally {
        setCartLoading(false);
      }
    };
    fetchCart();
  }, [setCart]);

  // Show loading while cart is being fetched
  if (cartLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8 text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">Loading cart...</p>
      </div>
    );
  }

  // Redirect if cart is empty (after loading is complete)
  if (!cart || !cart.items || cart.items.length === 0) {
    navigate('/cart');
    return null;
  }

  /**
   * Handle shipping information submission
   * Creates order on backend and initializes Razorpay order
   */
  const handleShippingSubmit = async (e) => {
    e.preventDefault();
    console.log('Submitting shipping info:', shippingInfo);
    setLoading(true);

    try {
      // Create order on backend (stock validation, price locking)
      console.log('Calling /orders/checkout API...');
      const { data } = await api.post('/orders/checkout', shippingInfo);
      console.log('Checkout response:', data);
      
      // Set Razorpay order response for payment form
      setRazorpayOrderResponse(data);
      
      toast.success('Order created! Proceed to payment.');
    } catch (error) {
      console.error('Order creation error:', error);
      console.error('Error response:', error.response?.data);
      const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Failed to create order. Please try again.';
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handlePaymentSuccess = () => {
    // Callback when payment is verified
    toast.success('Order confirmed!');
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-3xl font-bold mb-8">Checkout</h1>

      {!razorpayOrderResponse ? (
        // Shipping Information Form
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-6">Shipping Information</h2>
          <form onSubmit={handleShippingSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Address <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                value={shippingInfo.shippingAddress}
                onChange={(e) => setShippingInfo({ ...shippingInfo, shippingAddress: e.target.value })}
                className="input-field"
                placeholder="Enter your full address"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  City <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={shippingInfo.shippingCity}
                  onChange={(e) => setShippingInfo({ ...shippingInfo, shippingCity: e.target.value })}
                  className="input-field"
                  placeholder="Enter city"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Country <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={shippingInfo.shippingCountry}
                  onChange={(e) => setShippingInfo({ ...shippingInfo, shippingCountry: e.target.value })}
                  className="input-field"
                  placeholder="Enter country"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Zip Code <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={shippingInfo.shippingZipCode}
                  onChange={(e) => setShippingInfo({ ...shippingInfo, shippingZipCode: e.target.value })}
                  className="input-field"
                  placeholder="Enter zip code"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Phone <span className="text-red-500">*</span>
                </label>
                <input
                  type="tel"
                  required
                  value={shippingInfo.shippingPhone}
                  onChange={(e) => setShippingInfo({ ...shippingInfo, shippingPhone: e.target.value })}
                  className="input-field"
                  placeholder="Enter phone number"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full btn-primary disabled:opacity-50 mt-6"
            >
              {loading ? 'Processing...' : 'Continue to Payment'}
            </button>
          </form>
        </div>
      ) : (
        // Razorpay Payment Form
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-6">Payment Details</h2>
          <RazorpayPaymentForm 
            razorpayOrderResponse={razorpayOrderResponse}
            onPaymentSuccess={handlePaymentSuccess}
          />
          
          <button
            onClick={() => setRazorpayOrderResponse(null)}
            className="mt-4 text-blue-600 hover:text-blue-700 text-sm"
          >
            ← Change Shipping Address
          </button>
        </div>
      )}
    </div>
  );
}
