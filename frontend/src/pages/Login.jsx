import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api, { errorMessage } from '../api/client';
import BrandMark from '../components/BrandMark';

export default function Login() {
  const { login, loginWithToken } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [googleEnabled, setGoogleEnabled] = useState(false);

  useEffect(() => {
    const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    const oauthToken = fragment.get('token');
    const oauthError = fragment.get('error');
    if (oauthToken || oauthError) {
      window.history.replaceState({}, '', window.location.pathname + window.location.search);
    }
    if (oauthError) {
      setError(oauthError);
    }
    if (oauthToken) {
      (async () => {
        try {
          await loginWithToken(oauthToken);
          navigate('/dashboard');
        } catch (err) {
          setError(errorMessage(err));
        }
      })();
    }

    (async () => {
      try {
        const response = await api.get('/auth/providers');
        setGoogleEnabled(!!response?.data?.google);
      } catch {
        setGoogleEnabled(false);
      }
    })();
  }, []);

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
        <div className="login-mark">
          <BrandMark size={40} />
        </div>
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

        {googleEnabled && (
          <button
            className="btn secondary google-btn"
            type="button"
            style={{ width: '100%', marginTop: 10 }}
            onClick={() => window.location.assign('/api/oauth2/authorization/google')}
          >
            Continue with Google
          </button>
        )}
      </form>
    </div>
  );
}
