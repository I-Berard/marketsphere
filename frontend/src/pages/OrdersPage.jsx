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
    <div className="wrap">
      <h1 className="page-title">Your orders</h1>
      {orders.length === 0 ? <div className="empty">You have not placed an order yet.</div> : null}
      {orders.map((o) => (
        <article className="order-card" key={o.id}>
          <div className="section-head">
            <h2 style={{ fontSize: 18 }}>{o.orderNumber}</h2>
            <span className={`status-pill ${o.status.toLowerCase()}`}>{o.status.replaceAll("_", " ")}</span>
          </div>
          <div className="muted">{o.placedAt ? new Date(o.placedAt).toLocaleString() : ""} · {money(o.totalRwf)}</div>
          <ul>
            {(o.lines ?? []).map((line) => (
              <li key={line.productId + line.sku}>{line.qty} × {line.name} — {money(line.unitPriceRwf)}</li>
            ))}
          </ul>
          <p className="muted">
            Payment {o.payment?.status ?? "pending"}
            {o.shipment ? ` · ${o.shipment.state} ${o.shipment.trackingNo}` : ""}
          </p>
          {(o.status === "PLACED" || o.status === "PAYMENT_TIMED_OUT") ? (
            <button className="btn btn-primary" type="button" onClick={() => retry(o.id)}>Retry payment</button>
          ) : null}
        </article>
      ))}
      {error ? <p className="error">{error}</p> : null}
    </div>
  );
}
