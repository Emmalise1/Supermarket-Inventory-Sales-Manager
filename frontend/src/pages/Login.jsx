import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api, { errorMessage } from '../api/client';

/**
 * Sign-in page.
 *
 * <p>Two methods: the usual email/password form, and the OAuth2 "Continue with Google"
 * button (shown only when the backend reports it is configured). The Google flow comes
 * back to this page with the JWT in the URL <em>fragment</em>, which is stored and then
 * replaced by the real profile from GET /api/auth/me.</p>
 */
export default function Login() {
  const { login, loginWithToken } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [googleEnabled, setGoogleEnabled] = useState(false);

  useEffect(() => {
    // 1. finish an OAuth2 redirect: #token=... or #error=...
    const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    const oauthToken = fragment.get('token');
    const oauthError = fragment.get('error');
    if (oauthToken || oauthError) {
      // keep credentials out of the address bar and out of the history
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

    // 2. ask the backend which sign-in methods are configured
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
