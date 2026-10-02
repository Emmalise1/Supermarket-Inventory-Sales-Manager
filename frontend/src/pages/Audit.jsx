import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatDateTime } from '../utils/format';

/** Audit history from MongoDB (admin/manager only). */
export default function Audit() {
  const [entries, setEntries] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/audit')
      .then((res) => setEntries(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  return (
    <div className="page">
      <h1>Audit history</h1>
      <p className="subtitle">Who did what and when - stored in MongoDB (latest 50 entries).</p>

      {error && <div className="alert error">{error}</div>}

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>When</th><th>Actor</th><th>Role</th><th>Action</th><th>Entity</th><th>Details</th></tr>
            </thead>
            <tbody>
              {entries.map((e) => (
                <tr key={e.id}>
                  <td>{formatDateTime(e.createdAt)}</td>
                  <td>{e.actorEmail || '-'}</td>
                  <td>{e.role ? <span className="badge role">{e.role}</span> : '-'}</td>
                  <td><strong>{e.action}</strong></td>
                  <td>{e.entityType} #{e.entityId}</td>
                  <td>{e.details}</td>
                </tr>
              ))}
              {entries.length === 0 && (
                <tr><td colSpan="6" className="empty-state">No audit entries yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
