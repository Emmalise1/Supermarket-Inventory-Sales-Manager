import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatDateTime } from '../utils/format';
import { useAuth } from '../context/AuthContext';

export default function Inventory() {
  const { hasRole } = useAuth();
  const canManage = hasRole('ADMIN', 'MANAGER');

  const [movements, setMovements] = useState([]);
  const [products, setProducts] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [receipt, setReceipt] = useState({ productId: '', quantity: 10, reference: '', note: '' });
  const [adjustment, setAdjustment] = useState({ productId: '', quantityChange: -1, reason: '' });

  function load() {
    api.get('/inventory/movements')
      .then((res) => setMovements(res.data))
      .catch((err) => setError(errorMessage(err)));
    api.get('/products')
      .then((res) => setProducts(res.data))
      .catch(() => {});
  }

  useEffect(load, []);

  async function submitReceipt(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    try {
      const { data } = await api.post('/inventory/goods-receiving', {
        productId: Number(receipt.productId),
        quantity: Number(receipt.quantity),
        reference: receipt.reference || null,
        note: receipt.note || null,
      });
      setNotice(`Received stock - '${data.name}' now has ${data.quantityInStock} units.`);
      setReceipt({ productId: '', quantity: 10, reference: '', note: '' });
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function submitAdjustment(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    try {
      const { data } = await api.post('/inventory/adjustments', {
        productId: Number(adjustment.productId),
        quantityChange: Number(adjustment.quantityChange),
        reason: adjustment.reason,
      });
      setNotice(`Adjusted - '${data.name}' now has ${data.quantityInStock} units.`);
      setAdjustment({ productId: '', quantityChange: -1, reason: '' });
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="page">
      <h1>Inventory</h1>
      <p className="subtitle">Goods receiving, stock adjustments and the full movement history.</p>

      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}

      {canManage && (
        <div className="grid cols-2">
          <form className="card" onSubmit={submitReceipt}>
            <h2>Goods receiving (+stock)</h2>
            <label className="field">
              Product
              <select value={receipt.productId} onChange={(e) => setReceipt({ ...receipt, productId: e.target.value })} required>
                <option value="">Select product...</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.barcode})</option>)}
              </select>
            </label>
            <div className="form-row">
              <label className="field">
                Quantity received
                <input type="number" min="1" value={receipt.quantity}
                  onChange={(e) => setReceipt({ ...receipt, quantity: e.target.value })} required />
              </label>
              <label className="field">
                Delivery reference
                <input value={receipt.reference} onChange={(e) => setReceipt({ ...receipt, reference: e.target.value })}
                  placeholder="PO-2026-001" />
              </label>
            </div>
            <label className="field">
              Note
              <input value={receipt.note} onChange={(e) => setReceipt({ ...receipt, note: e.target.value })} />
            </label>
            <button className="btn" type="submit">Receive goods</button>
          </form>

          <form className="card" onSubmit={submitAdjustment}>
            <h2>Stock adjustment (+/-)</h2>
            <label className="field">
              Product
              <select value={adjustment.productId}
                onChange={(e) => setAdjustment({ ...adjustment, productId: e.target.value })} required>
                <option value="">Select product...</option>
                {products.map((p) => <option key={p.id} value={p.id}>{p.name} (stock {p.quantityInStock})</option>)}
              </select>
            </label>
            <div className="form-row">
              <label className="field">
                Change (+/-)
                <input type="number" value={adjustment.quantityChange}
                  onChange={(e) => setAdjustment({ ...adjustment, quantityChange: e.target.value })} required />
              </label>
              <label className="field">
                Reason *
                <input value={adjustment.reason} onChange={(e) => setAdjustment({ ...adjustment, reason: e.target.value })}
                  placeholder="Damaged / expired / correction" required />
              </label>
            </div>
            <button className="btn" type="submit">Apply adjustment</button>
            <p className="subtitle" style={{ marginTop: 10 }}>
              An adjustment can never make stock negative - the backend rejects it.
            </p>
          </form>
        </div>
      )}

      <div className="card">
        <h2>Movement history (latest 100)</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>When</th><th>Type</th><th>Product</th><th>Change</th>
                <th>Current stock</th><th>Reason</th><th>User</th>
              </tr>
            </thead>
            <tbody>
              {movements.map((m) => (
                <tr key={m.id}>
                  <td>{formatDateTime(m.createdAt)}</td>
                  <td>
                    <span className={`badge ${m.movementType === 'GOODS_RECEIPT' ? 'ok' : m.movementType === 'SALE' ? 'db' : 'low'}`}>
                      {m.movementType}
                    </span>
                  </td>
                  <td>{m.productName}</td>
                  <td style={{ color: m.quantityChange < 0 ? '#d64545' : '#12a150', fontWeight: 600 }}>
                    {m.quantityChange > 0 ? `+${m.quantityChange}` : m.quantityChange}
                  </td>
                  <td>{m.currentStock}</td>
                  <td>{m.reason || '-'}</td>
                  <td>#{m.userId}</td>
                </tr>
              ))}
              {movements.length === 0 && (
                <tr><td colSpan="7" className="empty-state">No stock movements yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
