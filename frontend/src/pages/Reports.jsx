import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatMoney } from '../utils/format';

export default function Reports() {
  const [range, setRange] = useState({ from: '', to: '' });
  const [report, setReport] = useState(null);
  const [error, setError] = useState('');

  function load(from, to) {
    setError('');
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    api.get(`/reports/sales?${params.toString()}`)
      .then((res) => setReport(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - 6);
    const toDate = to.toISOString().slice(0, 10);
    const fromDate = from.toISOString().slice(0, 10);
    setRange({ from: fromDate, to: toDate });
    load(fromDate, toDate);
  }, []);

  return (
    <div className="page">
      <h1>Reports</h1>
      <p className="subtitle">Sales performance read directly from MySQL (fresh data, never cached).</p>

      {error && <div className="alert error">{error}</div>}

      <div className="card">
        <form
          className="form-row"
          onSubmit={(e) => {
            e.preventDefault();
            load(range.from, range.to);
          }}
        >
          <label className="field">
            From
            <input type="date" value={range.from} onChange={(e) => setRange({ ...range, from: e.target.value })} />
          </label>
          <label className="field">
            To
            <input type="date" value={range.to} onChange={(e) => setRange({ ...range, to: e.target.value })} />
          </label>
          <button className="btn" type="submit" style={{ alignSelf: 'flex-end' }}>Generate</button>
        </form>
      </div>

      {report && (
        <>
          <div className="grid cols-4">
            <div className="stat accent">
              <div className="label">Revenue</div>
              <div className="value">{formatMoney(report.totalRevenue)}</div>
            </div>
            <div className="stat">
              <div className="label">Sales</div>
              <div className="value">{report.saleCount}</div>
            </div>
            <div className="stat good">
              <div className="label">Average sale</div>
              <div className="value">{formatMoney(report.averageSale)}</div>
            </div>
            <div className="stat">
              <div className="label">Period</div>
              <div className="value" style={{ fontSize: 16 }}>{report.from} → {report.to}</div>
            </div>
          </div>

          <div className="card" style={{ marginTop: 18 }}>
            <h2>Top products</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>#</th><th>Product</th><th>Units sold</th><th>Revenue</th></tr>
                </thead>
                <tbody>
                  {report.topProducts.map((p, index) => (
                    <tr key={p.productId}>
                      <td>{index + 1}</td>
                      <td>{p.productName}</td>
                      <td>{p.quantitySold}</td>
                      <td>{formatMoney(p.revenue)}</td>
                    </tr>
                  ))}
                  {report.topProducts.length === 0 && (
                    <tr><td colSpan="4" className="empty-state">No sales in this period.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
