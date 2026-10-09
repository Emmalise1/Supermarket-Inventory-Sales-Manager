import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { formatPrice, cacheLabel } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import emptySearch from '../assets/illustrations/empty-search.svg';

export default function Products() {
  const { hasRole } = useAuth();
  const canManage = hasRole('ADMIN', 'MANAGER');
  const [searchParams, setSearchParams] = useSearchParams();

  const [products, setProducts] = useState([]);
  const [branches, setBranches] = useState([]);
  const [categories, setCategories] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [branchFilter, setBranchFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [lookup, setLookup] = useState(null);
  const [benchmark, setBenchmark] = useState(null);
  const [lookupError, setLookupError] = useState('');

  const emptyForm = {
    barcode: '', name: '', price: '', costPrice: '', quantityInStock: 0,
    lowStockThreshold: 10, categoryId: '', supplierId: '', branchId: '', active: true,
  };
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);

  function load() {
    api.get('/products')
      .then((res) => setProducts(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
    api.get('/branches').then((r) => setBranches(r.data)).catch(() => {});
    api.get('/categories').then((r) => setCategories(r.data)).catch(() => {});
    api.get('/suppliers').then((r) => setSuppliers(r.data)).catch(() => {});
  }, []);

  useEffect(() => {
    if (searchParams.get('add') === '1' && canManage) {
      setShowForm(true);
      setEditingId(null);
      setForm(emptyForm);
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, canManage, setSearchParams]);

  function stockStatus(product) {
    if (product.quantityInStock === 0) return 'out';
    if (product.quantityInStock <= 20) return 'low';
    return 'healthy';
  }

  function statusPill(product) {
    const status = stockStatus(product);
    if (status === 'out') return <span className="badge off">Out of stock</span>;
    if (status === 'low') return <span className="badge low">Low stock</span>;
    return <span className="badge ok">Active</span>;
  }

  const query = search.trim().toLowerCase();
  const filtered = products.filter((p) => {
    const matchesQuery = !query
      || p.name.toLowerCase().includes(query)
      || p.barcode.includes(query);
    const matchesCategory = !categoryFilter || String(p.categoryId) === categoryFilter;
    const matchesBranch = !branchFilter || String(p.branchId) === branchFilter;
    const matchesStatus = !statusFilter || stockStatus(p) === statusFilter;
    return matchesQuery && matchesCategory && matchesBranch && matchesStatus;
  });

  async function handleLookup(event) {
    event.preventDefault();
    if (!search.trim()) return;
    setLookupError('');
    setLookup(null);
    setBenchmark(null);
    try {
      const { data } = await api.get(`/products/barcode/${encodeURIComponent(search.trim())}`);
      setLookup(data);
    } catch (err) {
      setLookupError(errorMessage(err));
    }
  }

  async function runBenchmark() {
    if (!lookup) return;
    setLookupError('');
    try {
      const { data } = await api.get(
        `/products/barcode/${encodeURIComponent(lookup.product.barcode)}/benchmark?iterations=20`
      );
      setBenchmark(data);
    } catch (err) {
      setLookupError(errorMessage(err));
    }
  }

  function startEdit(product) {
    setEditingId(product.id);
    setForm({
      barcode: product.barcode,
      name: product.name,
      price: String(product.price),
      costPrice: product.costPrice != null ? String(product.costPrice) : '',
      quantityInStock: product.quantityInStock,
      lowStockThreshold: product.lowStockThreshold,
      categoryId: product.categoryId || '',
      supplierId: product.supplierId || '',
      branchId: product.branchId,
      active: product.active,
    });
    setShowForm(true);
  }

  async function submitForm(event) {
    event.preventDefault();
    setError('');
    setNotice('');
    const payload = {
      barcode: form.barcode,
      name: form.name,
      price: Number(form.price),
      costPrice: form.costPrice === '' ? null : Number(form.costPrice),
      quantityInStock: Number(form.quantityInStock),
      lowStockThreshold: Number(form.lowStockThreshold),
      categoryId: form.categoryId === '' ? null : Number(form.categoryId),
      supplierId: form.supplierId === '' ? null : Number(form.supplierId),
      branchId: Number(form.branchId),
      active: !!form.active,
    };
    try {
      if (editingId) {
        await api.put(`/products/${editingId}`, payload);
        setNotice('Product updated.');
      } else {
        await api.post('/products', payload);
        setNotice('Product created.');
      }
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function deactivate(product) {
    setError('');
    try {
      await api.delete(`/products/${product.id}`);
      setNotice('Product deactivated.');
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="page">
      <h1>Products</h1>
      <p className="subtitle">Product catalog across branches.</p>

      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}

      <form className="toolbar" onSubmit={handleLookup}>
        <label className="field toolbar-search">
          Find product by barcode
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Scan a barcode or type a name, press Enter"
          />
        </label>
        <label className="field">
          Category
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
            <option value="">All categories</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label className="field">
          Branch
          <select value={branchFilter} onChange={(e) => setBranchFilter(e.target.value)}>
            <option value="">All branches</option>
            {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </label>
        <label className="field">
          Status
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            <option value="healthy">Active</option>
            <option value="low">Low stock</option>
            <option value="out">Out of stock</option>
          </select>
        </label>
        {canManage && (
          <button
            className="btn toolbar-action"
            type="button"
            onClick={() => { setShowForm(!showForm); setEditingId(null); setForm(emptyForm); }}
          >
            {showForm ? 'Close' : 'Add Product'}
          </button>
        )}
      </form>

      {(lookup || lookupError) && (
        <div className="card">
          {lookupError && <div className="alert error">{lookupError}</div>}
          {lookup && (
            <>
              <div className="alert info">
                <strong>{lookup.product.name}</strong> - {formatPrice(lookup.product.price)} - stock{' '}
                {lookup.product.quantityInStock}
                <br />
                Answered by <span className={`badge ${lookup.cacheHit ? 'cache' : 'db'}`}>
                  {cacheLabel(lookup.cacheHit)}
                </span>{' '}
                in {lookup.elapsedMs} ms
              </div>

              {canManage && (
                <button className="btn secondary" onClick={runBenchmark}>
                  Run performance comparison (MySQL vs Redis)
                </button>
              )}

              {benchmark && (
                <div className="alert success" style={{ marginTop: 12 }}>
                  <strong>Measured</strong> over {benchmark.iterations} iterations:
                  <br />
                  Without cache (MySQL): <strong>{benchmark.avgDbLookupMs} ms</strong> per lookup
                  <br />
                  With cache (Redis): <strong>{benchmark.avgRedisLookupMs} ms</strong> per lookup
                  <br />
                  Speed-up: <strong>{benchmark.speedup}x</strong>
                  <br />
                  <small>{benchmark.note}</small>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {showForm && (
        <div className="card">
          <h2>{editingId ? 'Edit product' : 'Add product'}</h2>
          <form onSubmit={submitForm}>
            <div className="form-row">
              <label className="field">
                Barcode *
                <input value={form.barcode} onChange={(e) => setForm({ ...form, barcode: e.target.value })} required />
              </label>
              <label className="field">
                Name *
                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </label>
            </div>
            <div className="form-row">
              <label className="field">
                Price (RWF) *
                <input type="number" step="0.01" min="0" value={form.price}
                  onChange={(e) => setForm({ ...form, price: e.target.value })} required />
              </label>
              <label className="field">
                Cost price (RWF)
                <input type="number" step="0.01" min="0" value={form.costPrice}
                  onChange={(e) => setForm({ ...form, costPrice: e.target.value })} />
              </label>
              <label className="field">
                Initial stock
                <input type="number" min="0" value={form.quantityInStock}
                  onChange={(e) => setForm({ ...form, quantityInStock: e.target.value })} />
              </label>
            </div>
            <div className="form-row">
              <label className="field">
                Low-stock threshold
                <input type="number" min="0" value={form.lowStockThreshold}
                  onChange={(e) => setForm({ ...form, lowStockThreshold: e.target.value })} />
              </label>
              <label className="field">
                Category
                <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
                  <option value="">-</option>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </label>
              <label className="field">
                Supplier
                <select value={form.supplierId} onChange={(e) => setForm({ ...form, supplierId: e.target.value })}>
                  <option value="">-</option>
                  {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </label>
            </div>
            <div className="form-row">
              <label className="field">
                Branch *
                <select value={form.branchId} onChange={(e) => setForm({ ...form, branchId: e.target.value })} required>
                  <option value="">-</option>
                  {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </label>
              <label className="field">
                Status
                <select value={form.active ? '1' : '0'}
                  onChange={(e) => setForm({ ...form, active: e.target.value === '1' })}>
                  <option value="1">Active</option>
                  <option value="0">Inactive</option>
                </select>
              </label>
            </div>
            <button className="btn" type="submit">{editingId ? 'Save changes' : 'Create product'}</button>
          </form>
        </div>
      )}

      <div className="card">
        <h2>Catalog ({filtered.length})</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Barcode</th><th>Name</th><th>Branch</th><th>Price</th>
                <th>Stock</th><th>Status</th>{canManage && <th></th>}
              </tr>
            </thead>
            <tbody>
              {filtered.map((p) => (
                <tr key={p.id}>
                  <td>{p.barcode}</td>
                  <td>{p.name}</td>
                  <td>#{p.branchId}</td>
                  <td>{formatPrice(p.price)}</td>
                  <td>{p.quantityInStock}</td>
                  <td>{statusPill(p)}</td>
                  {canManage && (
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button className="btn small secondary" onClick={() => startEdit(p)}>Edit</button>{' '}
                      {p.active && (
                        <button className="btn small danger" onClick={() => deactivate(p)}>Deactivate</button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan="7" className="empty-state">
                    <img className="empty-illustration" src={emptySearch} alt="" />
                    <div>No products match your search.</div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
