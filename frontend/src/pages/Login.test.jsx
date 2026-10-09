import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Login from './Login';
import { AuthProvider } from '../context/AuthContext';
import api from '../api/client';

vi.mock('../api/client', () => {
  const apiInstance = {
    post: vi.fn(),
    get: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  };
  return {
    __esModule: true,
    default: apiInstance,
    errorMessage: (error) => error?.response?.data?.message || error?.message || 'Unexpected error',
  };
});

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('Login page', () => {
  beforeEach(() => {
    localStorage.clear();
    api.post.mockReset();
    api.get.mockReset();
    window.location.hash = '';
  });

  it('renders the login form without any demo credentials', () => {
    renderLogin();
    expect(screen.getByText('SUPERmarket')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('admin@supermarket.rw')).toBeInTheDocument();
    expect(screen.queryByText(/Demo accounts/)).toBeNull();
    expect(screen.queryByText(/Admin@123/)).toBeNull();
    expect(screen.queryByText(/Manager@123/)).toBeNull();
    expect(screen.queryByText(/Cashier@123/)).toBeNull();
  });

  it('stores the JWT and navigates on successful login', async () => {
    api.post.mockResolvedValue({
      data: {
        token: 'jwt-token-123',
        tokenType: 'Bearer',
        user: { id: 1, fullName: 'System Admin', role: 'ADMIN', branchId: null },
      },
    });

    renderLogin();
    fireEvent.change(screen.getByPlaceholderText('admin@supermarket.rw'), {
      target: { value: 'admin@supermarket.rw' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), { target: { value: 'Admin@123' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(screen.getByText('Dashboard page')).toBeInTheDocument());
    expect(localStorage.getItem('token')).toBe('jwt-token-123');
    expect(api.post).toHaveBeenCalledWith('/auth/login', {
      email: 'admin@supermarket.rw',
      password: 'Admin@123',
    });
  });

  it('shows the backend error message on failed login', async () => {
    api.post.mockRejectedValue({ response: { data: { message: 'Invalid email or password' } } });

    renderLogin();
    fireEvent.change(screen.getByPlaceholderText('admin@supermarket.rw'), {
      target: { value: 'wrong@x.rw' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), { target: { value: 'nope' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() =>
      expect(screen.getByText('Invalid email or password')).toBeInTheDocument()
    );
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('shows the Google button only when the backend reports it is configured', async () => {
    api.get.mockResolvedValue({ data: { password: true, google: true } });

    renderLogin();

    expect(await screen.findByRole('button', { name: /continue with google/i })).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith('/auth/providers');
  });

  it('hides the Google button when OAuth2 credentials are not configured', async () => {
    api.get.mockResolvedValue({ data: { password: true, google: false } });

    renderLogin();

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/auth/providers'));
    expect(screen.queryByRole('button', { name: /continue with google/i })).toBeNull();
  });

  it('finishes the OAuth2 redirect: stores the JWT from the fragment and loads the profile', async () => {
    window.location.hash = '#token=oauth-jwt-456';
    api.get.mockImplementation((url) =>
      url === '/auth/me'
        ? Promise.resolve({
            data: { id: 3, fullName: 'Google User', role: 'CASHIER', branchId: 2 },
          })
        : Promise.resolve({ data: { password: true, google: true } })
    );

    renderLogin();

    await waitFor(() => expect(localStorage.getItem('token')).toBe('oauth-jwt-456'));
    await waitFor(() => expect(screen.getByText('Dashboard page')).toBeInTheDocument());
    expect(window.location.hash).toBe('');
    expect(JSON.parse(localStorage.getItem('user')).fullName).toBe('Google User');
  });

  it('surfaces the error the OAuth2 redirect came back with', async () => {
    window.location.hash = '#error=No account is registered for you';
    api.get.mockResolvedValue({ data: { password: true, google: true } });

    renderLogin();

    expect(await screen.findByText('No account is registered for you')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });
});
