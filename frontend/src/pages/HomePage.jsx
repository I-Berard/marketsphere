import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { money } from "../api";
import { Media } from "../shell";
import { useStore } from "../store";

export function HomePage() {
  const { products, categories, query, refreshCatalog, addToCart, notify } = useStore();
  const [categoryId, setCategoryId] = useState("");

  useEffect(() => {
    const t = setTimeout(() => refreshCatalog(query).catch(() => {}), 180);
    return () => clearTimeout(t);
  }, [query, refreshCatalog]);

  const visible = useMemo(() => {
    const filtered = categoryId
      ? products.filter((p) => p.categoryId === categoryId)
      : products;
    return [...filtered].sort((a, b) => Number(b.stockBadge === "IN_STOCK") - Number(a.stockBadge === "IN_STOCK"));
  }, [products, categoryId]);

  return (
    <>
      <section className="bg-gradient-to-br from-primary via-secondary to-[#7ad7f7] text-white py-20 lg:py-28 text-center md:text-left rounded-3xl mb-12 shadow-[0_20px_40px_-15px_rgba(0,120,212,0.4)] relative overflow-hidden">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMzAiIGN5PSIzMCIgcj0iMiIgZmlsbD0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIi8+PC9zdmc+')] opacity-50"></div>
        <div className="max-w-6xl mx-auto px-8 w-full relative z-10">
          <p className="text-sm font-bold uppercase tracking-widest text-white/90 mb-4 inline-block">Rwanda · integer RWF</p>
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight mb-6 leading-tight">Shop Cloud Sphere</h1>
          <p className="text-lg md:text-xl text-white/90 max-w-2xl mb-10 mx-auto md:mx-0">Cables and accessories for everyday work. Gold members ship free. Buy 5+ of a line for 10% off.</p>
          <div className="flex flex-wrap gap-4 justify-center md:justify-start">
            <a className="btn bg-white text-primary hover:bg-gray-50 shadow-lg" href="#catalogue">Browse catalogue</a>
            <Link className="btn border border-white/70 text-white hover:bg-white/10" to="/cart">View cart</Link>
          </div>
        </div>
      </section>

      <section className="bg-surface rounded-2xl py-10 px-8 mb-16 border border-gray-100 shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          <div>
            <strong className="block font-bold text-lg mb-2 text-text-main">Free shipping</strong>
            <p className="text-text-muted text-sm leading-relaxed m-0">Gold members, or baskets of 50,000 RWF and above.</p>
          </div>
          <div>
            <strong className="block font-bold text-lg mb-2 text-text-main">Bulk discount</strong>
            <p className="text-text-muted text-sm leading-relaxed m-0">10% off any line when you add five or more.</p>
          </div>
          <div>
            <strong className="block font-bold text-lg mb-2 text-text-main">Pay locally</strong>
            <p className="text-text-muted text-sm leading-relaxed m-0">MTN MoMo or card, with prices in whole francs.</p>
          </div>
          <div>
            <strong className="block font-bold text-lg mb-2 text-text-main">Saved lists</strong>
            <p className="text-text-muted text-sm leading-relaxed m-0">Wishlist stays with your account across sessions.</p>
          </div>
        </div>
      </section>

      <section id="catalogue" className="mb-20">
        <div className="flex flex-col md:flex-row items-baseline justify-between gap-4 mb-8">
          <h2 className="text-3xl font-bold text-text-main m-0">Catalogue</h2>
          <span className="text-text-muted">{visible.length} published items</span>
        </div>
        
        <div className="flex flex-wrap gap-3 mb-10">
          <button type="button" className={`chip ${categoryId === "" ? "active" : ""}`} onClick={() => setCategoryId("")}>
            All
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              type="button"
              className={`chip ${categoryId === c.id ? "active" : ""}`}
              onClick={() => setCategoryId(c.id)}
            >
              {c.name}
            </button>
          ))}
        </div>
        
        {visible.length === 0 ? <div className="py-20 text-center text-text-muted text-lg">No products match that search.</div> : null}
        
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {visible.map((p) => {
            const inStock = p.stockBadge === "IN_STOCK";
            return (
              <article key={p.id} className="glass-card overflow-hidden flex flex-col group hover:-translate-y-1 hover:border-primary/30 hover:shadow-xl transition-all duration-300 cursor-pointer">
                <Link to={`/product/${p.id}`} className="flex flex-col flex-1">
                  <Media name={p.name} sku={p.sku} imageUrl={p.imageUrl} className="h-48 grid place-items-center text-white font-bold text-4xl group-hover:scale-105 transition-transform duration-500 ease-out" />
                  <div className="p-5 flex flex-col gap-1 flex-1 relative z-10 bg-background/50">
                    <div className="text-xs text-text-muted font-medium mb-1">{p.sku}</div>
                    <h3 className="font-bold text-lg text-text-main leading-tight line-clamp-2">{p.name}</h3>
                    <div className="font-bold text-xl text-primary mt-auto pt-2">{money(p.priceRwf)}</div>
                    <div className={`text-xs font-bold mt-1 ${inStock ? "text-success" : "text-danger"}`}>
                      {inStock ? `${p.stockQty} in stock` : "Out of stock"}
                    </div>
                  </div>
                </Link>
                <div className="px-5 pb-5 pt-0 bg-background/50 relative z-10">
                  <button
                    className="btn btn-primary w-full shadow-md hover:shadow-lg"
                    disabled={!inStock}
                    type="button"
                    onClick={async (e) => {
                      e.preventDefault();
                      try {
                        await addToCart(p.id, 1);
                      } catch (err) {
                        notify(err.message);
                      }
                    }}
                  >
                    Add to cart
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      </section>
    </>
  );
}
