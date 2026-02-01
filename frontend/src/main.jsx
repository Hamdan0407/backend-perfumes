import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import { GoogleOAuthProvider } from '@react-oauth/google';
import 'react-toastify/dist/ReactToastify.css';
import App from './App';
import './index.css';

// Get Google Client ID from environment variables
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '387518166476-a1q2r3s4t5u6v7w8x9y0z1a2b3c4d5e6.apps.googleusercontent.com';

// Warn if using placeholder client ID
if (GOOGLE_CLIENT_ID.includes('387518166476')) {
  console.warn('⚠️ Using placeholder Google Client ID. Please set VITE_GOOGLE_CLIENT_ID in .env file for Google OAuth to work.');
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <BrowserRouter>
        <App />
        <ToastContainer
          position="top-right"
          autoClose={3000}
          hideProgressBar={false}
          newestOnTop
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="light"
        />
      </BrowserRouter>
    </GoogleOAuthProvider>
  </React.StrictMode>
);

