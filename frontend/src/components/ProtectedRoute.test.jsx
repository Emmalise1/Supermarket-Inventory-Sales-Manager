import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { AuthProvider } from '../context/AuthContext';

function renderWithRole(user) {
  localStorage.clear();
  if (user) {
    localStorage.setItem('token', 'test-token');
    localStorage.setItem('user', JSON.stringify(user));
  }
  return render(
    <MemoryRouter initialEntries={['/reports']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>Login page</div>} />
          <Route element={<ProtectedRoute roles={['ADMIN', 'MANAGER']} />}>
            <Route path="/reports" element={<div>Reports page</div>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => localStorage.clear());

  it('redirects anonymous users to the login page', () => {
    renderWithRole(null);
    expect(screen.getByText('Login page')).toBeInTheDocument();
  });

  it('blocks users without the required role', () => {
    renderWithRole({ id: 3, fullName: 'Cashier', role: 'CASHIER', branchId: 1 });
    expect(screen.getByText('Access denied')).toBeInTheDocument();
    expect(screen.queryByText('Reports page')).not.toBeInTheDocument();
  });

  it('lets managers through', () => {
    renderWithRole({ id: 2, fullName: 'Manager', role: 'MANAGER', branchId: 1 });
    expect(screen.getByText('Reports page')).toBeInTheDocument();
  });
});
