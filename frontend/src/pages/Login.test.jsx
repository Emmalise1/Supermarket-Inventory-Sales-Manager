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
  });

  it('renders the login form and demo credentials', () => {
    renderLogin();
    expect(screen.getByText('SUPERmarket')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('admin@supermarket.rw')).toBeInTheDocument();
    expect(screen.getByText(/admin@supermarket\.rw/)).toBeInTheDocument();
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
});
