import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function ProductPage() {
  const { id } = useParams();
  const { call, addToCart, notify, user, categories } = useStore();
  const [product, setProduct] = useState(null);
  const [qty, setQty] = useState(1);
  const [error, setError] = useState("");

  useEffect(() => {
    call(`/products/${id}`)
      .then(({ data }) => setProduct(data))
      .catch((e) => setError(e.message));
  }, [id, call]);

  if (error) {
    return (
      <div className="max-w-6xl mx-auto w-full px-4 py-16">
        <p className="text-center text-text-muted text-lg mb-4">{error}</p>
        <div className="flex justify-center"><Link to="/" className="text-primary hover:underline">Back to shop</Link></div>
      </div>
    );
  }
  if (!product) return <div className="max-w-6xl mx-auto w-full px-4 py-20 text-center text-text-muted text-lg">Loading…</div>;

  const inStock = product.stockBadge === "IN_STOCK";
  const category = categories.find((c) => c.id === product.categoryId);

  return (
    <div className="max-w-6xl mx-auto w-full px-4 grid grid-cols-1 md:grid-cols-2 gap-10 lg:gap-16 py-10 lg:py-16">
      <Media name={product.name} sku={product.sku} imageUrl={product.imageUrl} className="rounded-3xl min-h-[420px] grid place-items-center text-white font-bold text-6xl shadow-sm border border-gray-100" />
      <div className="flex flex-col justify-center">
        <nav className="flex gap-2 flex-wrap text-sm text-text-muted mb-4 font-medium">
          <Link to="/" className="hover:text-primary transition-colors">Shop</Link>
          <span>/</span>
          {category ? <span className="text-text-main">{category.name}</span> : <span className="text-text-main">Catalogue</span>}
          <span>/</span>
          <span className="text-text-main">{product.name}</span>
        </nav>
        <div className="text-sm text-text-muted font-medium mb-1">{product.sku}</div>
        <h1 className="text-3xl md:text-4xl font-bold tracking-tight text-text-main mb-3">{product.name}</h1>
        <p className="text-text-muted mb-6 leading-relaxed text-lg">{product.description || "A Cloud Sphere catalogue item. Money is integer RWF."}</p>
        <div className="text-4xl font-bold text-primary mb-2">{money(product.priceRwf)}</div>
        <p className={`text-sm font-bold mb-4 ${inStock ? "text-success" : "text-danger"}`}>
          {inStock ? `${product.stockQty} available` : "Currently out of stock"}
        </p>
        <p className="text-sm text-text-muted bg-surface p-3 rounded-xl border border-gray-100">Quantity 5+ gets 10% off. Gold members, or baskets ≥ 50,000 RWF, ship free.</p>
        
        <div className="flex items-center gap-3 my-8">
          <button className="w-12 h-12 rounded-full border border-gray-200 flex items-center justify-center font-bold text-xl hover:bg-surface transition-colors active:scale-95" type="button" onClick={() => setQty((q) => Math.max(1, Number(q) - 1))}>−</button>
          <input type="number" min="1" max={product.stockQty || 1} value={qty} onChange={(e) => setQty(e.target.value)} aria-label="Quantity" className="w-20 text-center font-bold text-lg border border-gray-200 rounded-xl p-3 bg-background focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all" />
          <button className="w-12 h-12 rounded-full border border-gray-200 flex items-center justify-center font-bold text-xl hover:bg-surface transition-colors active:scale-95" type="button" onClick={() => setQty((q) => Number(q) + 1)}>+</button>
        </div>
        
        <div className="flex gap-4 flex-wrap">
          <button
            className="btn btn-primary px-8 py-3 text-lg flex-1 min-w-[200px] shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30"
            disabled={!inStock}
            type="button"
            onClick={async () => {
              try {
                await addToCart(product.id, Number(qty) || 1);
              } catch (e) {
                notify(e.message);
              }
            }}
          >
            Add to cart
          </button>
          {user ? (
            <button
              className="btn btn-ghost px-6 py-3 text-lg font-semibold"
              type="button"
              onClick={async () => {
                try {
                  await call(`/wishlist/${product.id}`, { method: "POST" });
                  notify("Saved to wishlist.");
                } catch (e) {
                  notify(e.message);
                }
              }}
            >
              ♥ Save
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}
