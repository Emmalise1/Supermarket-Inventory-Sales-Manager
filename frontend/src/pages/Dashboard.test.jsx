import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Dashboard from './Dashboard';
import { AuthProvider } from '../context/AuthContext';
import api from '../api/client';

vi.mock('../api/client', () => {
  const apiInstance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  };
  return {
    __esModule: true,
    default: apiInstance,
    errorMessage: (error) => error?.response?.data?.message || error?.message || 'Unexpected error',
  };
});

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = window.ResizeObserver || ResizeObserverStub;

const dashboardData = {
  productCount: 42,
  lowStockCount: 3,
  todayRevenue: 45000,
  todaySales: 12,
  fromCache: false,
  recentSaleNumbers: [],
};

const salesData = [
  {
    id: 1,
    saleNumber: 'INV-2026-0001',
    branchId: 1,
    cashierId: 3,
    totalAmount: 9000,
    itemCount: 4,
    status: 'COMPLETED',
    createdAt: new Date().toISOString(),
    items: [],
  },
];

const trendData = [
  { date: '2026-10-03', revenue: 100, salesCount: 2 },
  { date: '2026-10-04', revenue: 110, salesCount: 3 },
  { date: '2026-10-05', revenue: 90, salesCount: 1 },
  { date: '2026-10-06', revenue: 200, salesCount: 5 },
  { date: '2026-10-07', revenue: 150, salesCount: 4 },
  { date: '2026-10-08', revenue: 100, salesCount: 6 },
  { date: '2026-10-09', revenue: 110, salesCount: 3 },
];

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <AuthProvider>
        <Routes>
          <Route path="/dashboard" element={<Dashboard />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('Dashboard page', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('token', 'test-token');
    localStorage.setItem(
      'user',
      JSON.stringify({ id: 1, fullName: 'System Admin', role: 'ADMIN', branchId: null })
    );
    api.get.mockReset();
    api.get.mockImplementation((url) => {
      if (url === '/dashboard') return Promise.resolve({ data: dashboardData });
      if (url === '/sales') return Promise.resolve({ data: salesData });
      if (url.startsWith('/reports/trend')) return Promise.resolve({ data: trendData });
      return Promise.resolve({ data: {} });
    });
  });

  it('renders KPI cards, quick actions and the sales table', async () => {
    renderDashboard();

    expect(await screen.findByText('Sales Overview')).toBeInTheDocument();
    expect(screen.getByText('45,000 RWF')).toBeInTheDocument();
    expect(screen.getByText('Active products')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();

    expect(screen.getByRole('button', { name: /new sale/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add product/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /receive stock/i })).toBeInTheDocument();

    expect(await screen.findByText('INV-2026-0001')).toBeInTheDocument();
    expect(screen.getByText('Sale number')).toBeInTheDocument();
    expect(screen.getByText('Cashier')).toBeInTheDocument();
    expect(screen.getByText('Amount')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('When')).toBeInTheDocument();
    expect(screen.getByText('9,000 RWF')).toBeInTheDocument();
    expect(screen.getByText('Completed')).toBeInTheDocument();
    expect(screen.getByText('#3')).toBeInTheDocument();

    expect(screen.getByText('+10% vs yesterday')).toBeInTheDocument();
    expect(screen.queryByText(/redis/i)).toBeNull();
  });

  it('requests the trend endpoint when the range changes to 30 days', async () => {
    renderDashboard();

    expect(await screen.findByText('Sales Overview')).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith('/reports/trend?days=7');

    fireEvent.change(screen.getByDisplayValue('7 days'), { target: { value: '30' } });

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/reports/trend?days=30'));
  });
});
