import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatDateTime } from '../utils/format';

export default function Notifications() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');

  function load() {
    api.get('/notifications')
      .then((res) => setItems(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
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

  return (
    <div className="page">
      <h1>Notifications</h1>
      <p className="subtitle">Delivered through RabbitMQ, stored in MongoDB.</p>

      {error && <div className="alert error">{error}</div>}

      <div className="card">
        {items.length === 0 && <p className="empty-state">No notifications yet.</p>}
        {items.map((n) => (
          <div key={n.id} className={`notif ${n.read ? '' : 'unread'}`}>
            <div className="title">
              {n.title}{' '}
              <span className="badge role">{n.type}</span>
            </div>
            <div className="msg">{n.message}</div>
            <div className="time">{formatDateTime(n.createdAt)}</div>
            {!n.read && (
              <button className="btn small secondary" style={{ marginTop: 6 }} onClick={() => markRead(n.id)}>
                Mark as read
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
