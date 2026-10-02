import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage } from '../api/client';

/** Email/password login -> OAuth2 JWT from the Spring Boot backend. */
export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email.trim(), password);
      navigate('/dashboard');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={handleSubmit}>
        <h1>SUPERmarket</h1>
        <p className="sub">Inventory &amp; Sales Manager - sign in to continue</p>

        {error && <div className="alert error">{error}</div>}

        <label className="field">
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="admin@supermarket.rw"
            required
            autoFocus
          />
        </label>

        <label className="field">
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />
        </label>

        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Signing in...' : 'Sign in'}
        </button>

        <div className="demo-credentials">
          Demo accounts (seeded on first run):
          <br />
          <code>admin@supermarket.rw / Admin@123</code> (ADMIN)
          <br />
          <code>manager@supermarket.rw / Manager@123</code> (MANAGER)
          <br />
          <code>cashier@supermarket.rw / Cashier@123</code> (CASHIER)
        </div>
      </form>
    </div>
  );
}
