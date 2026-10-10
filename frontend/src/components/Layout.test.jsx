import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Layout from './Layout';
import { AuthProvider } from '../context/AuthContext';
import { BranchProvider } from '../context/BranchContext';
import api from '../api/client';

vi.mock('../api/client', () => {
  const apiInstance = {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  };
  return {
    __esModule: true,
    default: apiInstance,
    errorMessage: (error) => error?.response?.data?.message || error?.message || 'Unexpected error',
  };
});

function renderLayout() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <BranchProvider>
          <Layout />
        </BranchProvider>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('Layout shell', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('token', 'test-token');
    localStorage.setItem(
      'user',
      JSON.stringify({ id: 1, fullName: 'System Admin', role: 'ADMIN', branchId: null })
    );
    api.get.mockReset();
    api.get.mockImplementation((url) => {
      if (url === '/notifications/unread-count') return Promise.resolve({ data: { unread: 3 } });
      if (url === '/branches') return Promise.resolve({ data: [{ id: 1, name: 'Kigali Main' }] });
      return Promise.resolve({ data: {} });
    });
  });

  it('shows the unread count badge on the notification bell', async () => {
    renderLayout();
    const badge = await screen.findByText('3');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('count');
  });

  it('opens the profile dropdown and closes it on Escape', async () => {
    renderLayout();
    const trigger = document.querySelector('.profile-trigger');
    fireEvent.click(trigger);
    expect(screen.getByText('Account Settings')).toBeInTheDocument();
    expect(screen.getByText('Log out')).toBeInTheDocument();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByText('Account Settings')).toBeNull();
  });

  it('renders the branch selector with All branches and fetched branches', async () => {
    renderLayout();
    const option = await screen.findByRole('option', { name: 'Kigali Main' });
    expect(option).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'All branches' })).toBeInTheDocument();
  });

  it('renders an icon on every sidebar navigation item', () => {
    renderLayout();
    const icons = document.querySelectorAll('aside.sidebar a svg');
    expect(icons.length).toBeGreaterThanOrEqual(9);
  });
});
