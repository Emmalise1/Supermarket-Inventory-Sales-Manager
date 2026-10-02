import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';
import { formatMoney } from '../utils/format';

/**
 * POS / Sales screen: scan or type a barcode (served by the Redis cache),
 * build the cart and check out. The backend validates stock and prevents
 * negative stock.
 */
export default function Pos() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
  const [barcode, setBarcode] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/products')
      .then((res) => setProducts(res.data.filter((p) => p.active)))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  const total = cart.reduce((sum, line) => sum + line.unitPrice * line.quantity, 0);

  async function addByBarcode(event) {
    event.preventDefault();
    if (!barcode.trim()) return;
    setError('');
    setSuccess('');
    try {
      const { data } = await api.get(`/products/barcode/${encodeURIComponent(barcode.trim())}`);
      addProduct(data.product, data.cacheHit);
      setBarcode('');
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function addProduct(product, fromCache) {
    if (product.branchId && cart.length > 0 && cart[0].branchId !== product.branchId) {
      setError('All items in one sale must belong to the same branch.');
      return;
    }
    setCart((current) => {
      const existing = current.find((line) => line.productId === product.id);
      if (existing) {
        return current.map((line) =>
          line.productId === product.id ? { ...line, quantity: line.quantity + 1 } : line
        );
      }
      return [
        ...current,
        {
          productId: product.id,
          name: product.name,
          barcode: product.barcode,
          branchId: product.branchId,
          unitPrice: Number(product.price),
          quantity: 1,
          stock: product.quantityInStock,
          fromCache: !!fromCache,
        },
      ];
    });
  }

  function changeQty(productId, delta) {
    setCart((current) =>
      current
        .map((line) => (line.productId === productId ? { ...line, quantity: line.quantity + delta } : line))
        .filter((line) => line.quantity > 0)
    );
  }

  async function checkout() {
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      const { data } = await api.post('/sales', {
        branchId: cart[0]?.branchId,
        items: cart.map((line) => ({ productId: line.productId, quantity: line.quantity })),
      });
      setSuccess(`Sale ${data.saleNumber} recorded: ${formatMoney(data.totalAmount)} (${data.itemCount} items)`);
      setCart([]);
      const res = await api.get('/products');
      setProducts(res.data.filter((p) => p.active));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <h1>POS / Sales</h1>
      <p className="subtitle">Scan barcodes (answered by Redis) and check out. Stock is validated server-side.</p>

      {error && <div className="alert error">{error}</div>}
      {success && <div className="alert success">{success}</div>}

      <div className="pos-grid">
        <div className="card">
          <h2>Add items</h2>
          <form onSubmit={addByBarcode} className="form-row">
            <label className="field" style={{ flex: 1 }}>
              Barcode
              <input
                value={barcode}
                onChange={(e) => setBarcode(e.target.value)}
                placeholder="Scan or type a barcode, press Enter"
                autoFocus
              />
            </label>
            <button className="btn" type="submit" style={{ alignSelf: 'flex-end' }}>Add</button>
          </form>

          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Barcode</th><th>Name</th><th>Price</th><th>Stock</th><th></th></tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id} style={{ cursor: 'pointer' }} onClick={() => addProduct(p, false)}>
                    <td>{p.barcode}</td>
                    <td>{p.name}</td>
                    <td>{formatMoney(p.price)}</td>
                    <td>{p.quantityInStock}</td>
                    <td><button className="btn small secondary">Add</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <h2>Cart</h2>
          {cart.length === 0 ? (
            <p className="subtitle">Cart is empty.</p>
          ) : (
            <>
              {cart.map((line) => (
                <div className="cart-line" key={line.productId}>
                  <span className="name">
                    {line.name}
                    {line.fromCache && <span className="badge cache" style={{ marginLeft: 8 }}>Redis</span>}
                    <br />
                    <small style={{ color: '#67718b' }}>
                      {formatMoney(line.unitPrice)} · stock {line.stock}
                    </small>
                  </span>
                  <button className="btn small secondary" onClick={() => changeQty(line.productId, -1)}>-</button>
                  <strong>{line.quantity}</strong>
                  <button className="btn small secondary" onClick={() => changeQty(line.productId, 1)}>+</button>
                  <span style={{ width: 90, textAlign: 'right' }}>{formatMoney(line.unitPrice * line.quantity)}</span>
                </div>
              ))}
              <div className="cart-total">
                <span>Total</span>
                <span>{formatMoney(total)}</span>
              </div>
              <button className="btn" style={{ width: '100%', marginTop: 14 }} onClick={checkout}
                disabled={loading || cart.length === 0}>
                {loading ? 'Processing...' : 'Complete sale'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
