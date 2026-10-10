import { useEffect, useState } from "react";
import { money } from "../api";
import { useStore } from "../store";

export function OrdersPage() {
  const { call, notify } = useStore();
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");

  async function load() {
    const { data } = await call("/orders");
    setOrders(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, []);

  async function retry(id) {
    try {
      const { data } = await call(`/orders/${id}/pay`, {
        method: "POST",
        body: { paymentMethod: "MTN_MOMO" },
      });
      notify(`Payment ${data.payment?.status ?? data.status}`);
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <div className="max-w-6xl mx-auto w-full px-4 py-8 md:py-12">
      <h1 className="text-3xl font-bold text-text-main mb-8 tracking-tight">Your orders</h1>
      {orders.length === 0 ? <div className="text-text-muted py-16 text-center text-lg bg-surface rounded-3xl border border-gray-100">You have not placed an order yet.</div> : null}
      
      <div className="grid gap-6">
        {orders.map((o) => {
          const isPaid = o.status === "PAID" || o.status === "DELIVERED" || o.status === "SHIPPED";
          const isDanger = o.status === "CANCELLED" || o.status === "PAYMENT_TIMED_OUT";
          
          return (
            <article className="glass-card p-6 md:p-8 shadow-sm border border-gray-100 hover:shadow-md transition-shadow" key={o.id}>
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
                <h2 className="font-bold text-xl text-text-main m-0">{o.orderNumber}</h2>
                <span className={`w-fit px-3 py-1.5 text-xs font-bold uppercase tracking-wider rounded-full ${isPaid ? 'bg-success/10 text-success' : isDanger ? 'bg-danger/10 text-danger' : 'bg-primary/10 text-primary'}`}>
                  {o.status.replaceAll("_", " ")}
                </span>
              </div>
              
              <div className="text-text-muted text-sm font-medium mb-6 pb-6 border-b border-gray-100">
                {o.placedAt ? new Date(o.placedAt).toLocaleString() : ""} · <span className="font-bold text-text-main">{money(o.totalRwf)}</span>
              </div>
              
              <ul className="space-y-3 mb-6">
                {(o.lines ?? []).map((line) => (
                  <li key={line.productId + line.sku} className="flex gap-4 items-start">
                    <span className="font-bold text-text-muted bg-surface px-2 py-0.5 rounded text-sm">{line.qty} ×</span>
                    <span className="font-medium flex-1 text-text-main">{line.name}</span>
                    <span className="font-semibold text-text-main">{money(line.unitPriceRwf)}</span>
                  </li>
                ))}
              </ul>
              
              <div className="bg-surface p-4 rounded-xl flex flex-col sm:flex-row sm:items-center justify-between gap-4 border border-gray-100">
                <p className="text-sm font-medium m-0 text-text-main">
                  Payment <span className={`font-semibold ${o.payment?.status === 'SUCCEEDED' ? 'text-success' : 'text-primary'}`}>{o.payment?.status ?? "pending"}</span>
                  {o.shipment ? <span className="text-text-muted ml-2">· {o.shipment.state} <span className="font-mono text-xs bg-white px-2 py-1 rounded ml-1">{o.shipment.trackingNo}</span></span> : ""}
                </p>
                {(o.status === "PLACED" || o.status === "PAYMENT_TIMED_OUT") ? (
                  <button className="btn btn-primary py-2 px-5 text-sm shadow-md hover:shadow-lg" type="button" onClick={() => retry(o.id)}>Retry payment</button>
                ) : null}
              </div>
            </article>
          );
        })}
      </div>
      
      {error ? <div className="text-danger text-sm bg-danger/10 p-4 rounded-xl border border-danger/20 font-semibold mt-6">{error}</div> : null}
    </div>
  );
}
