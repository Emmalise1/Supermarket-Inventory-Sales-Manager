import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatDateTime } from '../utils/format';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [branches, setBranches] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const empty = { email: '', password: '', fullName: '', role: 'CASHIER', branchId: '', active: true };
  const [form, setForm] = useState(empty);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  function load() {
    api.get('/users')
      .then((res) => setUsers(res.data))
      .catch((err) => setError(errorMessage(err)));
    api.get('/branches').then((r) => setBranches(r.data)).catch(() => {});
  }

  useEffect(load, []);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    const payload = {
      email: form.email,
      fullName: form.fullName,
      role: form.role,
      branchId: form.branchId === '' ? null : Number(form.branchId),
      active: !!form.active,
      password: form.password || null,
    };
    try {
      if (editingId) {
        await api.put(`/users/${editingId}`, payload);
        setNotice('User updated.');
      } else {
        await api.post('/users', payload);
        setNotice('User created.');
      }
      setForm(empty);
      setEditingId(null);
      setShowForm(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function startEdit(user) {
    setEditingId(user.id);
    setForm({
      email: user.email,
      password: '',
      fullName: user.fullName,
      role: user.role,
      branchId: user.branchId || '',
      active: user.active,
    });
    setShowForm(true);
  }

  return (
    <div className="page">
      <h1>Users</h1>
      <p className="subtitle">RBAC roles: ADMIN, MANAGER, CASHIER - each bound to a branch.</p>

      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>Accounts ({users.length})</h2>
          <button className="btn secondary" onClick={() => { setShowForm(!showForm); setEditingId(null); setForm(empty); }}>
            {showForm ? 'Close' : 'New user'}
          </button>
        </div>

        {showForm && (
          <form onSubmit={submit} style={{ marginTop: 16 }}>
            <div className="form-row">
              <label className="field">
                Full name *
                <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
              </label>
              <label className="field">
                Email *
                <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
              </label>
            </div>
            <div className="form-row">
              <label className="field">
                Password {editingId ? '(leave blank to keep)' : '*'}
                <input type="password" value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  required={!editingId} minLength={6} />
              </label>
              <label className="field">
                Role
                <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                  <option value="ADMIN">ADMIN</option>
                  <option value="MANAGER">MANAGER</option>
                  <option value="CASHIER">CASHIER</option>
                </select>
              </label>
              <label className="field">
                Branch
                <select value={form.branchId} onChange={(e) => setForm({ ...form, branchId: e.target.value })}>
                  <option value="">-</option>
                  {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </label>
            </div>
            <button className="btn" type="submit">{editingId ? 'Save changes' : 'Create user'}</button>
          </form>
        )}
      </div>

      <div className="card">
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Name</th><th>Email</th><th>Role</th><th>Branch</th><th>Status</th><th>Created</th><th></th></tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td><span className="badge role">{u.role}</span></td>
                  <td>{u.branchId ? `#${u.branchId}` : 'All'}</td>
                  <td>{u.active ? <span className="badge ok">Active</span> : <span className="badge off">Disabled</span>}</td>
                  <td>{formatDateTime(u.createdAt)}</td>
                  <td><button className="btn small secondary" onClick={() => startEdit(u)}>Edit</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
