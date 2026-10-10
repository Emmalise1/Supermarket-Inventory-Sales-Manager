import { useEffect, useState } from 'react';
import { Bell, TriangleAlert, ShoppingCart, Truck, RefreshCw } from 'lucide-react';
import api, { errorMessage } from '../api/client';
import { formatRelative } from '../utils/format';
import emptyNotifications from '../assets/illustrations/empty-notifications.svg';

function typeMeta(type) {
  if (type === 'LOW_STOCK') return { icon: TriangleAlert, tone: 'warn' };
  if (type === 'SALE_COMPLETED' || (type && type.includes('SALE'))) {
    return { icon: ShoppingCart, tone: 'good' };
  }
  if (type === 'GOODS_RECEIVED') return { icon: Truck, tone: 'info' };
  if (type === 'STOCK_ADJUSTED') return { icon: RefreshCw, tone: 'db' };
  return { icon: Bell, tone: 'db' };
}

const FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'unread', label: 'Unread' },
  { id: 'low', label: 'Low Stock' },
  { id: 'sales', label: 'Sales' },
];

export default function Notifications() {
  const [items, setItems] = useState([]);
  const [branches, setBranches] = useState([]);
  const [filter, setFilter] = useState('all');
  const [error, setError] = useState('');

  function load() {
    api.get('/notifications')
      .then((res) => setItems(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
    api.get('/branches')
      .then((res) => setBranches(res.data))
      .catch(() => {});
    const id = setInterval(load, 20000);
    return () => clearInterval(id);
  }, []);

  async function markRead(id) {
    try {
      await api.patch(`/notifications/${id}/read`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function branchName(branchId) {
    if (branchId === null || branchId === undefined) return 'All branches';
    const branch = branches.find((b) => b.id === branchId);
    return branch ? branch.name : `Branch #${branchId}`;
  }

  const filtered = items.filter((n) => {
    if (filter === 'unread') return !n.read;
    if (filter === 'low') return n.type === 'LOW_STOCK';
    if (filter === 'sales') return n.type && n.type.includes('SALE');
    return true;
  });

  return (
    <div className="page">
      <h1>Notifications</h1>
      <p className="subtitle">Delivered through RabbitMQ, stored in MongoDB.</p>

      {error && <div className="alert error">{error}</div>}

      <div className="card">
        <div className="filter-tabs">
          {FILTERS.map((tab) => (
            <button
              key={tab.id}
              className={`filter-tab ${filter === tab.id ? 'active' : ''}`}
              onClick={() => setFilter(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {items.length === 0 && (
          <div className="notif-empty">
            <img className="empty-illustration" src={emptyNotifications} alt="" />
            <p>
              You&apos;re all caught up. Notifications about low stock, sales,
              and system events will appear here.
            </p>
          </div>
        )}

        {items.length > 0 && filtered.length === 0 && (
          <p className="empty-state">No notifications match this filter.</p>
        )}

        {filtered.map((n) => {
          const meta = typeMeta(n.type);
          const Icon = meta.icon;
          return (
            <div key={n.id} className={`notif-card ${n.read ? '' : 'unread'}`}>
              <div className={`notif-icon ${meta.tone}`}>
                <Icon size={18} strokeWidth={2} />
              </div>
              <div className="notif-body">
                <div className="notif-title">{n.title}</div>
                <div className="notif-msg">{n.message}</div>
                <div className="notif-meta">
                  <span>{formatRelative(n.createdAt)}</span>
                  <span>{branchName(n.branchId)}</span>
                </div>
              </div>
              {!n.read && (
                <button className="btn small secondary" onClick={() => markRead(n.id)}>
                  Mark as read
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
