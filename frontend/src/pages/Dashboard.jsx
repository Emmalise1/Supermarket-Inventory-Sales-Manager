import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import {
  Banknote,
  ShoppingCart,
  Package,
  TriangleAlert,
  PackagePlus,
  Truck,
} from 'lucide-react';
import api, { errorMessage } from '../api/client';
import { formatPrice, formatRelative } from '../utils/format';
import { useAuth } from '../context/AuthContext';

function deltaOf(today, yesterday) {
  if (yesterday === undefined || yesterday === null || yesterday === 0) return null;
  const change = ((today - yesterday) / yesterday) * 100;
  if (!Number.isFinite(change)) return null;
  return Math.round(change);
}

function Delta({ value }) {
  if (value === null) return null;
  const positive = value >= 0;
  return (
    <div className={`delta ${positive ? 'up' : 'down'}`}>
      <span className="dot" />
      {positive ? '+' : ''}{value}% vs yesterday
    </div>
  );
}

export default function Dashboard() {
  const { user, hasRole } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [sales, setSales] = useState([]);
  const [trend, setTrend] = useState([]);
  const [days, setDays] = useState(7);
  const canSeeTrend = hasRole('ADMIN', 'MANAGER');

  useEffect(() => {
    api
      .get('/dashboard')
      .then((res) => setData(res.data))
      .catch((err) => setError(errorMessage(err)));
    api
      .get('/sales')
      .then((res) => setSales(res.data))
      .catch(() => setSales([]));
  }, []);

  useEffect(() => {
    if (!canSeeTrend) return;
    api
      .get(`/reports/trend?days=${days}`)
      .then((res) => setTrend(res.data))
      .catch(() => setTrend([]));
  }, [days, canSeeTrend]);

  if (error) return <div className="page"><div className="alert error">{error}</div></div>;
  if (!data) return <div className="page"><p className="subtitle">Loading dashboard...</p></div>;

  const todayPoint = trend.length > 0 ? trend[trend.length - 1] : null;
  const yesterdayPoint = trend.length > 1 ? trend[trend.length - 2] : null;
  const revenueDelta = todayPoint && yesterdayPoint
    ? deltaOf(Number(todayPoint.revenue), Number(yesterdayPoint.revenue))
    : null;
  const salesDelta = todayPoint && yesterdayPoint
    ? deltaOf(todayPoint.salesCount, yesterdayPoint.salesCount)
    : null;

  const recent = sales.slice(0, 10);

  function statusPill(status) {
    if (status === 'COMPLETED') return <span className="badge ok">Completed</span>;
    if (status === 'PENDING') return <span className="badge low">Pending</span>;
    if (status === 'REFUNDED' || status === 'CANCELLED') return <span className="badge off">Refunded</span>;
    return <span className="badge db">{status || '-'}</span>;
  }

  return (
    <div className="page">
      <h1>Dashboard</h1>
      <p className="subtitle">
        {user?.branchId ? `Branch #${user.branchId}` : 'All branches'} · summary of today&apos;s activity
      </p>

      <div className="grid cols-4">
        <div className="stat accent">
          <div className="label">
            Today&apos;s revenue
            <Banknote size={18} strokeWidth={2} className="stat-icon" />
          </div>
          <div className="value num">{formatPrice(data.todayRevenue)}</div>
          <Delta value={revenueDelta} />
        </div>
        <div className="stat good">
          <div className="label">
            Sales today
            <ShoppingCart size={18} strokeWidth={2} className="stat-icon" />
          </div>
          <div className="value num">{data.todaySales}</div>
          <Delta value={salesDelta} />
        </div>
        <div className="stat">
          <div className="label">
            Active products
            <Package size={18} strokeWidth={2} className="stat-icon" />
          </div>
          <div className="value num">{data.productCount}</div>
        </div>
        <div className="stat warn">
          <div className="label">
            Low-stock products
            <TriangleAlert size={18} strokeWidth={2} className="stat-icon" />
          </div>
          <div className="value num">{data.lowStockCount}</div>
        </div>
      </div>

      <div className="quick-actions">
        <button className="btn" onClick={() => navigate('/pos')}>
          <ShoppingCart size={16} strokeWidth={2} />
          New Sale
        </button>
        <button className="btn secondary" onClick={() => navigate('/products?add=1')}>
          <PackagePlus size={16} strokeWidth={2} />
          Add Product
        </button>
        <button className="btn secondary" onClick={() => navigate('/inventory?receive=1')}>
          <Truck size={16} strokeWidth={2} />
          Receive Stock
        </button>
      </div>

      {canSeeTrend && (
        <div className="card">
          <div className="card-head">
            <h2>Sales Overview</h2>
            <select
              className="days-select"
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
            >
              <option value={7}>7 days</option>
              <option value={30}>30 days</option>
            </select>
          </div>
          {trend.length === 0 ? (
            <p className="subtitle">No sales data for this period.</p>
          ) : (
            <div className="chart-box">
              <ResponsiveContainer width="100%" height={280}>
                <LineChart data={trend} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={(v) => String(v).slice(5)} />
                  <YAxis tickFormatter={(v) => v.toLocaleString('en-US')} />
                  <Tooltip
                    formatter={(value) => [formatPrice(value), 'Revenue']}
                    labelFormatter={(label) => `Date: ${label}`}
                  />
                  <Line
                    type="monotone"
                    dataKey="revenue"
                    stroke="#2154d8"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      )}

      <div className="card">
        <h2>Recent sales</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Sale number</th>
                <th>Cashier</th>
                <th className="num">Amount</th>
                <th>Status</th>
                <th>When</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((sale) => (
                <tr key={sale.id}>
                  <td>{sale.saleNumber}</td>
                  <td>#{sale.cashierId ?? '-'}</td>
                  <td className="num">{formatPrice(sale.totalAmount)}</td>
                  <td>{statusPill(sale.status)}</td>
                  <td>{formatRelative(sale.createdAt)}</td>
                </tr>
              ))}
              {recent.length === 0 && (
                <tr>
                  <td colSpan="5" className="empty-state">No sales recorded yet today.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
