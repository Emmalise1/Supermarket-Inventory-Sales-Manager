import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function Suppliers() {
  const { hasRole } = useAuth();
  const canManage = hasRole('ADMIN', 'MANAGER');

  const [suppliers, setSuppliers] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ name: '', contactPerson: '', phone: '', email: '' });
  const [showForm, setShowForm] = useState(false);

  function load() {
    api.get('/suppliers')
      .then((res) => setSuppliers(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(load, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    try {
      await api.post('/suppliers', form);
      setForm({ name: '', contactPerson: '', phone: '', email: '' });
      setShowForm(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="page">
      <h1>Suppliers</h1>
      <p className="subtitle">Companies we buy stock from.</p>

      {error && <div className="alert error">{error}</div>}

      {canManage && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0 }}>Directory ({suppliers.length})</h2>
            <button className="btn secondary" onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Close' : 'New supplier'}
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
                  Contact person
                  <input value={form.contactPerson} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} />
                </label>
              </div>
              <div className="form-row">
                <label className="field">
                  Phone
                  <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
                </label>
                <label className="field">
                  Email
                  <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                </label>
              </div>
              <button className="btn" type="submit">Create supplier</button>
            </form>
          )}
        </div>
      )}

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Name</th><th>Contact person</th><th>Phone</th><th>Email</th><th>Status</th></tr>
            </thead>
            <tbody>
              {suppliers.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>{s.contactPerson || '-'}</td>
                  <td>{s.phone || '-'}</td>
                  <td>{s.email || '-'}</td>
                  <td>{s.active ? <span className="badge ok">Active</span> : <span className="badge off">Inactive</span>}</td>
                </tr>
              ))}
              {suppliers.length === 0 && (
                <tr><td colSpan="5" className="empty-state">No suppliers yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
