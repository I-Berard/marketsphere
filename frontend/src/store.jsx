import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, storage } from "./api";

const Store = createContext(null);

export function StoreProvider({ children }) {
  const [theme, setTheme] = useState(() => storage.theme() || "light");
  const [token, setToken] = useState(() => storage.token());
  const [user, setUser] = useState(null);
  const [cartToken, setCartToken] = useState(() => storage.cart());
  const [cart, setCart] = useState(null);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [query, setQuery] = useState("");
  const [toast, setToast] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === "system") root.removeAttribute("data-theme");
    else root.setAttribute("data-theme", theme);
    storage.setTheme(theme === "system" ? "" : theme);
  }, [theme]);

  const rememberCart = useCallback((next, headerToken) => {
    setCart(next);
    if (headerToken) {
      setCartToken(headerToken);
      storage.setCart(headerToken);
    } else if (next?.guestToken) {
      setCartToken(next.guestToken);
      storage.setCart(next.guestToken);
    }
  }, []);

  const call = useCallback(
    (path, opts = {}) => api(path, { ...opts, token, cartToken: opts.cartToken ?? cartToken }),
    [token, cartToken]
  );

  const notify = useCallback((msg) => {
    setToast(msg);
    window.setTimeout(() => setToast(""), 2800);
  }, []);

  const refreshCart = useCallback(async () => {
    const { data, cartToken: header } = await call("/cart");
    rememberCart(data, header);
    return data;
  }, [call, rememberCart]);

  const refreshCatalog = useCallback(
    async (q = query) => {
      const qs = new URLSearchParams({ size: "20" });
      if (q) qs.set("q", q);
      const [{ data: page }, { data: cats }] = await Promise.all([
        call(`/products?${qs}`),
        call("/categories"),
      ]);
      setProducts(page.content ?? []);
      setCategories(cats ?? []);
    },
    [call, query]
  );

  const refreshMe = useCallback(async () => {
    if (!token) {
      setUser(null);
      return;
    }
    try {
      const { data } = await call("/auth/me");
      setUser(data);
    } catch {
      storage.setToken("");
      setToken("");
      setUser(null);
    }
  }, [call, token]);

  useEffect(() => {
    refreshCatalog().catch(() => {});
    refreshCart().catch(() => {});
    refreshMe().catch(() => {});
  }, [refreshCatalog, refreshCart, refreshMe]);

  const login = async (email, password) => {
    const { data, cartToken: header } = await call("/auth/login", {
      method: "POST",
      body: { email, password },
    });
    storage.setToken(data.accessToken);
    setToken(data.accessToken);
    setUser(data.user);
    const nextGuest = header || cartToken;
    if (header) {
      setCartToken(header);
      storage.setCart(header);
    }
    const cartRes = await api("/cart", { token: data.accessToken, cartToken: nextGuest });
    rememberCart(cartRes.data, cartRes.cartToken);
    notify("You're signed in.");
    return data.user;
  };

  const register = async (payload) => {
    const { data, cartToken: header } = await call("/auth/register", { method: "POST", body: payload });
    storage.setToken(data.accessToken);
    setToken(data.accessToken);
    setUser(data.user);
    const nextGuest = header || cartToken;
    if (header) {
      setCartToken(header);
      storage.setCart(header);
    }
    const cartRes = await api("/cart", { token: data.accessToken, cartToken: nextGuest });
    rememberCart(cartRes.data, cartRes.cartToken);
    notify("Welcome to Cloud Sphere.");
    return data.user;
  };

  const logout = async () => {
    try {
      await call("/auth/logout", { method: "POST" });
    } catch {
      /* token may already be invalid */
    }
    storage.setToken("");
    setToken("");
    setUser(null);
    notify("Signed out.");
  };

  const addToCart = async (productId, qty = 1) => {
    const { data, cartToken: header } = await call("/cart/items", {
      method: "POST",
      body: { productId, qty },
    });
    rememberCart(data, header);
    notify("Added to cart.");
    return data;
  };

  const updateQty = async (productId, qty) => {
    const { data } = await call(`/cart/items/${productId}`, {
      method: "PATCH",
      body: { qty },
    });
    rememberCart(data);
  };

  const applyCode = async (code) => {
    const { data } = await call("/cart/discount", { method: "POST", body: { code } });
    rememberCart(data);
    notify("Discount applied.");
  };

  const checkout = async (paymentMethod, stubMode) => {
    const { data } = await call("/checkout", {
      method: "POST",
      headers: stubMode ? { "X-Payment-Stub-Mode": stubMode, "Idempotency-Key": crypto.randomUUID() } : { "Idempotency-Key": crypto.randomUUID() },
      body: { paymentMethod, stubMode: stubMode || undefined },
    });
    await refreshCart();
    return data;
  };

  const value = useMemo(
    () => ({
      theme,
      setTheme,
      token,
      user,
      cart,
      products,
      categories,
      query,
      setQuery,
      toast,
      busy,
      setBusy,
      notify,
      call,
      login,
      register,
      logout,
      refreshCatalog,
      refreshCart,
      addToCart,
      updateQty,
      applyCode,
      checkout,
      isAdmin: user?.roles?.includes("ADMIN"),
    }),
    [theme, token, user, cart, products, categories, query, toast, busy, notify, call]
  );

  return <Store.Provider value={value}>{children}</Store.Provider>;
}

export function useStore() {
  const ctx = useContext(Store);
  if (!ctx) throw new Error("useStore must be used inside StoreProvider");
  return ctx;
}
