import { createContext, useContext, useState } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('user') || 'null');
    } catch {
      return null;
    }
  });

  async function login(email, password) {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    setToken(data.token);
    setUser(data.user);
    return data.user;
  }

  async function loginWithToken(oauthToken) {
    localStorage.setItem('token', oauthToken);
    setToken(oauthToken);
    try {
      const { data } = await api.get('/auth/me');
      localStorage.setItem('user', JSON.stringify(data));
      setUser(data);
      return data;
    } catch (err) {
      logout();
      throw err;
    }
  }

  function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }

  function hasRole(...roles) {
    return !!user && roles.includes(user.role);
  }

  return (
    <AuthContext.Provider value={{ token, user, login, loginWithToken, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
