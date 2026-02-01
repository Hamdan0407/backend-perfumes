import React, { useState } from 'react';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const res = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ email }),
      });
      if (!res.ok) throw new Error('Failed to send reset email');
      setSubmitted(true);
    } catch (err) {
      setError('Could not send reset email.');
    }
  };

  return (
    <div className="max-w-md mx-auto mt-16 p-6 bg-white rounded shadow">
      <h2 className="text-2xl font-bold mb-4">Forgot Password</h2>
      {submitted ? (
        <div className="text-green-600">If your email exists, a reset link has been sent.</div>
      ) : (
        <form onSubmit={handleSubmit}>
          <label className="block mb-2">Email</label>
          <input
            type="email"
            className="w-full border px-3 py-2 rounded mb-4"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
          {error && <div className="text-red-600 mb-2">{error}</div>}
          <button type="submit" className="w-full bg-blue-600 text-white py-2 rounded">Send Reset Link</button>
        </form>
      )}
    </div>
  );
}
