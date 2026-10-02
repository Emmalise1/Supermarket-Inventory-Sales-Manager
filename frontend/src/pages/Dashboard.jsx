import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatMoney } from '../utils/format';
import { useAuth } from '../context/AuthContext';

/**
 * Dashboard summary. The backend caches this summary in Redis for 5 minutes
 * - the "source" badge shows whether the answer came from Redis or MySQL.
 */
export default function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/dashboard')
      .then((res) => setData(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) return <div className="page"><div className="alert error">{error}</div></div>;
  if (!data) return <div className="page"><p className="subtitle">Loading dashboard...</p></div>;

  return (
    <div className="page">
      <h1>Dashboard</h1>
      <p className="subtitle">
        {user?.branchId ? `Branch #${user.branchId}` : 'All branches'} · summary cached in Redis for
        5 minutes{' '}
        {data.fromCache && <span className="badge cache">served from Redis</span>}
      </p>

      <div className="grid cols-4">
        <div className="stat accent">
          <div className="label">Today's revenue</div>
          <div className="value">{formatMoney(data.todayRevenue)}</div>
        </div>
        <div className="stat good">
          <div className="label">Sales today</div>
          <div className="value">{data.todaySales}</div>
        </div>
        <div className="stat">
          <div className="label">Active products</div>
          <div className="value">{data.productCount}</div>
        </div>
        <div className="stat warn">
          <div className="label">Low-stock products</div>
          <div className="value">{data.lowStockCount}</div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 18 }}>
        <h2>Recent sales</h2>
        {data.recentSaleNumbers?.length ? (
          <ul>
            {data.recentSaleNumbers.map((sale) => (
              <li key={sale}>{sale}</li>
            ))}
          </ul>
        ) : (
          <p className="subtitle">No sales recorded yet today.</p>
        )}
      </div>
    </div>
  );
}
