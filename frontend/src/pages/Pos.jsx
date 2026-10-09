import { useEffect, useState } from 'react';
import { X, ShoppingBasket } from 'lucide-react';
import api, { errorMessage } from '../api/client';
import { formatPrice } from '../utils/format';

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
  const discount = 0;

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

  function removeLine(productId) {
    setCart((current) => current.filter((line) => line.productId !== productId));
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
      setSuccess(`Sale ${data.saleNumber} recorded: ${formatPrice(data.totalAmount)} (${data.itemCount} items)`);
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
      <p className="subtitle">Scan barcodes and check out. Stock is validated server-side.</p>

      {error && <div className="alert error">{error}</div>}
      {success && <div className="alert success">{success}</div>}

      <div className="pos-grid">
        <div className="pos-left">
          <form className="pos-scan" onSubmit={addByBarcode}>
            <input
              className="pos-scan-input"
              value={barcode}
              onChange={(e) => setBarcode(e.target.value)}
              placeholder="Scan or type a barcode, press Enter"
              autoFocus
            />
            <button className="btn" type="submit">Add</button>
          </form>

          <div className="card">
            <h2>Products</h2>
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
                      <td>{formatPrice(p.price)}</td>
                      <td>{p.quantityInStock}</td>
                      <td><button className="btn small secondary">Add</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div className="card pos-cart">
          <h2>Cart</h2>
          {cart.length === 0 ? (
            <div className="cart-empty">
              <ShoppingBasket size={48} strokeWidth={1.5} />
              <p>Cart is empty. Scan a barcode to begin.</p>
            </div>
          ) : (
            <>
              {cart.map((line) => (
                <div className="cart-line" key={line.productId}>
                  <div className="cart-line-top">
                    <span className="name">
                      {line.name}
                      {line.fromCache && <span className="badge cache" style={{ marginLeft: 8 }}>Redis</span>}
                    </span>
                    <span className="line-total">{formatPrice(line.unitPrice * line.quantity)}</span>
                  </div>
                  <div className="cart-line-bottom">
                    <span className="meta">{formatPrice(line.unitPrice)} each</span>
                    <div className="qty-controls">
                      <button className="btn small secondary" onClick={() => changeQty(line.productId, -1)}>-</button>
                      <strong>{line.quantity}</strong>
                      <button className="btn small secondary" onClick={() => changeQty(line.productId, 1)}>+</button>
                      <button
                        className="btn small danger"
                        aria-label={`Remove ${line.name}`}
                        onClick={() => removeLine(line.productId)}
                      >
                        <X size={14} strokeWidth={2} />
                      </button>
                    </div>
                  </div>
                </div>
              ))}

              <div className="cart-summary">
                <div className="cart-row">
                  <span>Subtotal</span>
                  <span>{formatPrice(total)}</span>
                </div>
                <div className="cart-row">
                  <span>Discount</span>
                  <span>{formatPrice(discount)}</span>
                </div>
              </div>

              <div className="cart-total">
                <span>Total</span>
                <span>{formatPrice(total)}</span>
              </div>

              <button className="btn cart-checkout" onClick={checkout}
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
