import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { hueFrom, initials } from "./api";
import { useStore } from "./store";

export function Mark() {
  return <span className="w-9 h-9 rounded-full bg-primary text-white grid place-items-center font-bold shadow-[0_0_0_3px_rgba(0,120,212,0.2)]">S</span>;
}

export function Media({ name, sku, imageUrl, className = "rounded-xl min-h-[180px] grid place-items-center text-white font-bold text-4xl" }) {
  if (imageUrl) {
    return (
      <div className={`${className} bg-surface overflow-hidden`}>
        <img src={imageUrl} alt={name} className="w-full h-full object-contain mix-blend-multiply" />
      </div>
    );
  }
  const hue = hueFrom(sku || name);
  return (
    <div
      className={className}
      style={{ background: `linear-gradient(150deg, hsl(${hue} 85% 52%), hsl(199 100% 47%))` }}
    >
      {initials(name)}
    </div>
  );
}

function Icon({ d, stroke = 1.8 }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round">
      <path d={d} />
    </svg>
  );
}

export function Shell() {
  const { cart, user, logout, theme, setTheme, isAdmin, query, setQuery, refreshCatalog } = useStore();
  const qty = cart?.items?.reduce((n, i) => n + i.qty, 0) ?? 0;
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex flex-col bg-background text-text-main transition-colors duration-300">
      <header className="glass-header h-18">
        <div className="max-w-6xl w-full mx-auto px-4 h-full flex items-center gap-5">
          <Link className="flex items-center gap-2.5 font-bold text-lg shrink-0" to="/">
            <Mark />
            Market Sphere
          </Link>
          <form
            className="flex-1 hidden md:flex items-center gap-2 bg-surface border border-gray-200 rounded-full px-4 py-2 max-w-xl focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20 transition-all"
            onSubmit={(e) => {
              e.preventDefault();
              refreshCatalog(query);
              navigate("/");
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden className="text-text-muted">
              <circle cx="11" cy="11" r="7" />
              <path d="M20 20l-3-3" />
            </svg>
            <input
              className="bg-transparent border-none outline-none w-full text-text-main placeholder:text-text-muted text-sm"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search products, SKUs…"
              aria-label="Search products"
            />
          </form>
          <nav className="ml-auto flex items-center gap-1">
            <NavLink to="/" end className={({ isActive }) => `px-3 py-2 rounded-full font-semibold text-sm transition-colors ${isActive ? "text-primary bg-primary/10" : "hover:bg-surface text-text-main"}`}>Shop</NavLink>
            {user && <NavLink to="/orders" className={({ isActive }) => `px-3 py-2 rounded-full font-semibold text-sm transition-colors ${isActive ? "text-primary bg-primary/10" : "hover:bg-surface text-text-main"}`}>Orders</NavLink>}
            {user && <NavLink to="/wishlist" className={({ isActive }) => `px-3 py-2 rounded-full font-semibold text-sm transition-colors ${isActive ? "text-primary bg-primary/10" : "hover:bg-surface text-text-main"}`}>Wishlist</NavLink>}
            {isAdmin && <NavLink to="/admin" className={({ isActive }) => `px-3 py-2 rounded-full font-semibold text-sm transition-colors ${isActive ? "text-primary bg-primary/10" : "hover:bg-surface text-text-main"}`}>Admin</NavLink>}
            <Link to="/cart" className="relative w-10 h-10 rounded-full flex items-center justify-center hover:bg-surface transition-colors active:scale-95 text-text-main" aria-label="Cart" title="Cart">
              <Icon d="M6 7h15l-1.5 9h-12zM6 7 5 4H2" />
              {qty > 0 && <span className="absolute top-1 right-0 min-w-[18px] h-[18px] px-1 rounded-full bg-primary text-white text-[11px] font-bold grid place-items-center leading-none">{qty}</span>}
            </Link>
            <button
              className="relative w-10 h-10 rounded-full flex items-center justify-center hover:bg-surface transition-colors active:scale-95 text-text-main"
              title="Theme"
              onClick={() => setTheme(theme === "dark" ? "light" : theme === "light" ? "system" : "dark")}
            >
              <Icon d="M12 3a9 9 0 1 0 9 9 7 7 0 0 1-9-9z" />
            </button>
            {user ? (
              <button className="relative w-10 h-10 rounded-full flex items-center justify-center hover:bg-surface transition-colors active:scale-95 text-text-main" title={`Sign out ${user.email}`} onClick={() => { logout(); navigate("/"); }}>
                <span className="w-7 h-7 rounded-full bg-primary text-white grid place-items-center font-bold text-xs">{initials(user.email)}</span>
              </button>
            ) : (
              <Link to="/login" className="px-3 py-2 rounded-full font-semibold text-sm transition-colors hover:bg-surface text-text-main">Sign in</Link>
            )}
          </nav>
        </div>
      </header>
      <main className="flex-1 w-full max-w-6xl mx-auto px-4 py-8">
        <Outlet />
      </main>
      <footer className="border-t border-gray-200 py-8 text-text-muted text-sm mt-auto">
        <div className="max-w-6xl w-full mx-auto px-4 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-1">
            <div className="flex items-center gap-2 font-bold text-text-main mb-3">
              <Mark /> Market Sphere
            </div>
            <p className="leading-relaxed">Demo store for SPEDO501. Catalogue money is integer RWF. No native apps, no live payment rails.</p>
          </div>
          <div>
            <h3 className="uppercase tracking-wider font-bold mb-3 text-text-main">Shop</h3>
            <Link to="/" className="block mb-2 hover:text-primary transition-colors">Catalogue</Link>
            <Link to="/cart" className="block mb-2 hover:text-primary transition-colors">Cart</Link>
            <Link to="/wishlist" className="block mb-2 hover:text-primary transition-colors">Wishlist</Link>
          </div>
          <div>
            <h3 className="uppercase tracking-wider font-bold mb-3 text-text-main">Account</h3>
            {user ? <Link to="/orders" className="block mb-2 hover:text-primary transition-colors">Orders</Link> : <Link to="/login" className="block mb-2 hover:text-primary transition-colors">Sign in</Link>}
            {user ? <button type="button" className="block mb-2 hover:text-primary transition-colors text-left bg-transparent border-none p-0 cursor-pointer" onClick={() => { logout(); navigate("/"); }}>Sign out</button> : <Link to="/register" className="block mb-2 hover:text-primary transition-colors">Create account</Link>}
          </div>
          <div>
            <h3 className="uppercase tracking-wider font-bold mb-3 text-text-main">Fulfilment</h3>
            <p className="leading-relaxed">Gold members ship free. Standard shipping 2,000 RWF. Promo codes are 5–10 alphanumeric characters.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
