import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

function useQuery() {
  return new URLSearchParams(useLocation().search);
}

export default function ResetPassword() {
  const query = useQuery();
  const token = query.get('token');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!password || password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    try {
      const res = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ token, newPassword: password }),
      });
      if (!res.ok) throw new Error('Reset failed');
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError('Reset failed or token expired.');
    }
  };

  if (!token) return <div className="max-w-md mx-auto mt-16 p-6 bg-white rounded shadow">Invalid or missing token.</div>;

  return (
    <div className="max-w-md mx-auto mt-16 p-6 bg-white rounded shadow">
      <h2 className="text-2xl font-bold mb-4">Reset Password</h2>
      {success ? (
        <div className="text-green-600">Password reset! Redirecting to login...</div>
      ) : (
        <form onSubmit={handleSubmit}>
          <label className="block mb-2">New Password</label>
          <input
            type="password"
            className="w-full border px-3 py-2 rounded mb-4"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
          <label className="block mb-2">Confirm Password</label>
          <input
            type="password"
            className="w-full border px-3 py-2 rounded mb-4"
            value={confirm}
            onChange={e => setConfirm(e.target.value)}
            required
          />
          {error && <div className="text-red-600 mb-2">{error}</div>}
          <button type="submit" className="w-full bg-blue-600 text-white py-2 rounded">Reset Password</button>
        </form>
      )}
    </div>
  );
}
