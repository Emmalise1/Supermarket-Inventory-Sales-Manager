import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

/** Branch management. Creating/editing is ADMIN only. */
export default function Branches() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole('ADMIN');

  const [branches, setBranches] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', address: '', phone: '' });
  const [showForm, setShowForm] = useState(false);

  function load() {
    api.get('/branches')
      .then((res) => setBranches(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(load, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      await api.post('/branches', form);
      setForm({ name: '', address: '', phone: '' });
      setShowForm(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="page">
      <h1>Branches</h1>
      <p className="subtitle">Each user, product and sale is scoped to a branch.</p>

      {error && <div className="alert error">{error}</div>}

      {isAdmin && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0 }}>Branch list ({branches.length})</h2>
            <button className="btn secondary" onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Close' : 'New branch'}
            </button>
          </div>

          {showForm && (
            <form onSubmit={submit} style={{ marginTop: 16 }}>
              <div className="form-row">
                <label className="field">
                  Name *
                  <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
                </label>
                <label className="field">
                  Address
                  <input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
                </label>
                <label className="field">
                  Phone
                  <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
                </label>
              </div>
              <button className="btn" type="submit">Create branch</button>
            </form>
          )}
        </div>
      )}

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>#</th><th>Name</th><th>Address</th><th>Phone</th><th>Status</th></tr>
            </thead>
            <tbody>
              {branches.map((b) => (
                <tr key={b.id}>
                  <td>{b.id}</td>
                  <td>{b.name}</td>
                  <td>{b.address || '-'}</td>
                  <td>{b.phone || '-'}</td>
                  <td>{b.active ? <span className="badge ok">Active</span> : <span className="badge off">Closed</span>}</td>
                </tr>
              ))}
              {branches.length === 0 && (
                <tr><td colSpan="5" className="empty-state">No branches yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
