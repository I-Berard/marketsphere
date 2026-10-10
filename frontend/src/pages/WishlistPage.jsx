import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function WishlistPage() {
  const { call, addToCart, notify } = useStore();
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");

  async function load() {
    const { data } = await call("/wishlist");
    setItems(data);
  }

  useEffect(() => {
    load().catch((e) => setError(e.message));
  }, []);

  return (
    <div className="max-w-6xl mx-auto w-full px-4 py-8 md:py-12">
      <h1 className="text-3xl font-bold text-text-main mb-8 tracking-tight">Wishlist</h1>
      {items.length === 0 ? <div className="text-text-muted py-16 text-center text-lg bg-surface rounded-3xl border border-gray-100">Nothing saved. Browse the <Link to="/" className="text-primary hover:underline">shop</Link>.</div> : null}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        {items.map((w) => (
          <article className="glass-card overflow-hidden flex flex-col group hover:-translate-y-1 hover:shadow-xl transition-all duration-300" key={w.productId}>
            <Link to={`/product/${w.productId}`} className="flex flex-col flex-1">
              <Media name={w.name} sku={w.sku} imageUrl={w.imageUrl} className="h-48 grid place-items-center text-white font-bold text-4xl group-hover:scale-105 transition-transform duration-500 ease-out" />
              <div className="p-5 flex flex-col gap-1 flex-1 relative z-10 bg-background/50">
                <div className="text-xs text-text-muted font-medium mb-1">{w.sku}</div>
                <h3 className="font-bold text-lg text-text-main leading-tight line-clamp-2">{w.name}</h3>
                <div className="font-bold text-xl text-primary mt-auto pt-2">{money(w.priceRwf)}</div>
              </div>
            </Link>
            <div className="px-5 pb-5 pt-0 flex flex-col gap-2 relative z-10 bg-background/50">
              <button className="btn btn-primary w-full shadow-md hover:shadow-lg" type="button" onClick={() => addToCart(w.productId, 1).catch((e) => notify(e.message))}>
                Add to cart
              </button>
              <button
                className="btn btn-ghost w-full"
                type="button"
                onClick={async () => {
                  const { data } = await call(`/wishlist/${w.productId}`, { method: "DELETE" });
                  setItems(data);
                }}
              >
                Remove
              </button>
            </div>
          </article>
        ))}
      </div>
      {error ? <div className="text-danger text-sm bg-danger/10 p-4 rounded-xl border border-danger/20 font-semibold mt-6">{error}</div> : null}
    </div>
  );
}
