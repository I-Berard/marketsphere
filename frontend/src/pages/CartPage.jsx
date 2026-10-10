import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function CartPage() {
  const { cart, updateQty, applyCode, checkout, token, notify } = useStore();
  const [code, setCode] = useState("");
  const [method, setMethod] = useState("MTN_MOMO");
  const [stub, setStub] = useState("SUCCESS");
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const items = cart?.items ?? [];

  async function pay(e) {
    e.preventDefault();
    setError("");
    if (!token) {
      navigate("/login");
      return;
    }
    try {
      const placed = await checkout(method, stub);
      setOrder(placed);
      notify(placed.status === "PAID" ? "Order paid." : `Order ${placed.status.replaceAll("_", " ").toLowerCase()}`);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="max-w-6xl mx-auto w-full px-4">
      <div className="flex items-baseline justify-between mt-8 mb-6">
        <h1 className="text-3xl md:text-4xl font-bold text-text-main m-0 tracking-tight">Your cart</h1>
        <Link to="/" className="text-primary hover:underline font-medium">Continue shopping</Link>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_0.8fr] gap-10 py-4 mb-16">
        <div className="bg-white rounded-3xl p-2 sm:p-6 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100 h-fit">
          {items.length === 0 ? (
            <div className="text-text-muted py-16 text-center text-lg">
              Cart is empty. <Link to="/" className="text-primary hover:underline ml-1">Continue shopping</Link>
            </div>
          ) : null}
          {items.map((line) => (
            <div className="grid grid-cols-[80px_1fr_auto] sm:grid-cols-[100px_1fr_auto] gap-4 sm:gap-6 py-6 border-b border-gray-100 last:border-0 items-center" key={line.productId}>
              <Media name={line.name} sku={line.sku} imageUrl={line.imageUrl} className="w-20 h-20 sm:w-[100px] sm:h-[100px] rounded-2xl grid place-items-center text-white font-bold text-2xl shadow-sm border border-gray-100" />
              <div className="flex flex-col justify-center">
                <Link to={`/product/${line.productId}`} className="text-lg font-bold text-text-main hover:text-primary transition-colors line-clamp-1">{line.name}</Link>
                <div className="text-xs text-text-muted mt-1 font-medium bg-surface w-fit px-2 py-0.5 rounded-md">{line.sku}</div>
                <div className="text-text-muted text-sm mt-1">{money(line.unitPriceRwf)} each</div>
                <div className="flex items-center gap-1 sm:gap-3 mt-3">
                  <button className="w-8 h-8 sm:w-10 sm:h-10 rounded-full border border-gray-200 flex items-center justify-center font-bold hover:bg-surface transition-colors active:scale-95" type="button" onClick={() => updateQty(line.productId, line.qty - 1)}>−</button>
                  <span className="font-semibold w-6 text-center">{line.qty}</span>
                  <button className="w-8 h-8 sm:w-10 sm:h-10 rounded-full border border-gray-200 flex items-center justify-center font-bold hover:bg-surface transition-colors active:scale-95" type="button" onClick={() => updateQty(line.productId, line.qty + 1)}>+</button>
                  <button className="text-danger text-sm font-medium hover:underline ml-2" type="button" onClick={() => updateQty(line.productId, 0)}>Remove</button>
                </div>
              </div>
              <div className="font-bold text-lg sm:text-xl text-primary">{money(line.lineTotalRwf)}</div>
            </div>
          ))}
        </div>
        
        <aside className="bg-surface rounded-3xl p-8 h-fit lg:sticky lg:top-28 border border-gray-100 shadow-[0_8px_30px_rgb(0,0,0,0.04)]">
          <h2 className="text-2xl font-bold text-text-main mb-6 tracking-tight">Order summary</h2>
          {cart ? (
            <dl className="grid grid-cols-[1fr_auto] gap-x-4 gap-y-3 mb-6 text-text-main">
              <dt className="text-text-muted font-medium">Subtotal</dt><dd className="font-semibold">{money(cart.subtotalRwf)}</dd>
              <dt className="text-text-muted font-medium">Discount</dt><dd className="font-semibold text-success">− {money(cart.discountRwf)}</dd>
              <dt className="text-text-muted font-medium">Shipping</dt><dd className="font-semibold">{money(cart.shippingRwf)}</dd>
              <div className="col-span-2 my-2 border-t border-gray-200"></div>
              <dt className="font-bold text-xl">Total</dt><dd className="font-bold text-xl text-primary">{money(cart.totalRwf)}</dd>
            </dl>
          ) : null}
          <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-6 bg-white w-fit px-3 py-1.5 rounded-lg shadow-sm border border-gray-100">Priced as {cart?.pricedAsTier ?? "STANDARD"}</p>
          
          <form onSubmit={pay} className="space-y-5">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Promo code</label>
              <div className="flex gap-2">
                <input className="premium-input flex-1" value={code} onChange={(e) => setCode(e.target.value)} placeholder="SAVE2026" />
                <button className="btn bg-white border border-gray-200 shadow-sm hover:bg-gray-50 text-text-main" type="button" disabled={!code.trim()} onClick={() => applyCode(code).catch((e) => setError(e.message))}>
                  Apply
                </button>
              </div>
            </div>
            
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Payment</label>
              <select className="premium-input font-medium cursor-pointer" value={method} onChange={(e) => setMethod(e.target.value)}>
                <option value="MTN_MOMO">MTN MoMo</option>
                <option value="CARD">Credit / Debit Card</option>
              </select>
            </div>
            
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Gateway stub (demo)</label>
              <select className="premium-input font-medium cursor-pointer bg-white" value={stub} onChange={(e) => setStub(e.target.value)}>
                <option value="SUCCESS">SUCCESS</option>
                <option value="DECLINE">DECLINE</option>
                <option value="TIMEOUT">TIMEOUT</option>
              </select>
            </div>
            
            {error ? <div className="bg-danger/10 text-danger text-sm font-semibold p-3 rounded-xl border border-danger/20">{error}</div> : null}
            
            {order ? (
              <div className="bg-success/10 border border-success/20 p-4 rounded-xl text-sm">
                Placed <strong className="text-text-main">{order.orderNumber}</strong> — <span className="font-semibold text-success">{order.status}</span>.
                {" "}<Link to="/orders" className="text-primary hover:underline ml-1 font-medium">View orders</Link>
              </div>
            ) : null}
            
            <button className="btn btn-primary w-full py-4 text-lg shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30 mt-2" type="submit" disabled={items.length === 0}>
              {token ? "Place order" : "Sign in to checkout"}
            </button>
          </form>
        </aside>
      </div>
    </div>
  );
}
