import { useEffect, useMemo, useState } from "react";
import { money } from "../api";
import { compressProductPhoto } from "../image.js";
import { useStore } from "../store";

const NEW_CATEGORY = "__new__";
const STUDIO_PHONES = [
  { label: "Silver phone", url: "/products/phone-01.jpg" },
  { label: "Graphite phone", url: "/products/phone-02.jpg" },
  { label: "Sky phone", url: "/products/phone-03.jpg" },
  { label: "Champagne phone", url: "/products/phone-04.jpg" },
];

const emptyProduct = {
  sku: "",
  name: "",
  description: "",
  priceRwf: 15000,
  stockQty: 10,
  status: "PUBLISHED",
  category: "Cables",
  imageUrl: "",
};

export function AdminPage() {
  const { call, notify, categories, refreshCatalog } = useStore();
  const [orders, setOrders] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [form, setForm] = useState(emptyProduct);
  const [editingId, setEditingId] = useState(null);
  const [categoryChoice, setCategoryChoice] = useState("");
  const [newCategory, setNewCategory] = useState("");
  const [categoryDrafts, setCategoryDrafts] = useState({});
  const [freshCategory, setFreshCategory] = useState("");
  const [stockDrafts, setStockDrafts] = useState({});
  const [tracking, setTracking] = useState("CS-TRACK-001");
  const [error, setError] = useState("");
  const [compressing, setCompressing] = useState(false);

  async function loadOrders() {
    const { data } = await call("/admin/orders");
    setOrders(data);
  }

  async function loadCatalog() {
    const { data } = await call("/admin/products");
    setCatalog(data);
    setStockDrafts(Object.fromEntries(data.map((p) => [p.id, p.stockQty])));
  }

  async function reload() {
    await Promise.all([loadCatalog(), refreshCatalog(), loadOrders()]);
  }

  useEffect(() => {
    reload().catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (editingId || categoryChoice || !categories.length) return;
    const phones = categories.find((c) => c.name.toLowerCase() === "phones");
    setCategoryChoice((phones ?? categories[0]).name);
  }, [categories, categoryChoice, editingId]);

  const counts = useMemo(() => {
    const next = {};
    for (const p of catalog) {
      const key = p.categoryId || "none";
      next[key] = (next[key] || 0) + 1;
    }
    return next;
  }, [catalog]);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function startEdit(p) {
    setEditingId(p.id);
    setForm({
      sku: p.sku,
      name: p.name,
      description: p.description || "",
      priceRwf: p.priceRwf,
      stockQty: p.stockQty,
      status: p.status,
      category: p.categoryName || "",
      imageUrl: p.imageUrl || "",
    });
    setCategoryChoice(p.categoryName || NEW_CATEGORY);
    setNewCategory("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyProduct);
    setNewCategory("");
    setCategoryChoice(categories[0]?.name || "");
  }

  async function onPhoto(e) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setError("");
    setCompressing(true);
    try {
      const imageUrl = await compressProductPhoto(file);
      set("imageUrl", imageUrl);
      notify("Photo compressed to a light studio JPEG.");
    } catch (err) {
      setError(err.message);
    } finally {
      setCompressing(false);
    }
  }

  async function saveProduct(e) {
    e.preventDefault();
    setError("");
    const category = categoryChoice === NEW_CATEGORY ? newCategory.trim() : categoryChoice;
    if (!category) {
      setError("Choose a category or add a new one.");
      return;
    }
    const body = {
      ...form,
      category,
      imageUrl: form.imageUrl || null,
      priceRwf: Number(form.priceRwf),
      stockQty: Number(form.stockQty),
    };
    try {
      if (editingId) {
        await call(`/admin/products/${editingId}`, { method: "PATCH", body });
        notify("Product updated.");
      } else {
        await call("/admin/products", { method: "POST", body });
        notify("Product created.");
      }
      cancelEdit();
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function saveStock(p) {
    setError("");
    try {
      await call(`/admin/products/${p.id}/stock`, {
        method: "PATCH",
        body: { stockQty: Number(stockDrafts[p.id] ?? p.stockQty) },
      });
      notify(`Stock saved for ${p.sku}.`);
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function saveCategory(c) {
    const name = (categoryDrafts[c.id] ?? c.name).trim();
    if (!name || name === c.name) return;
    setError("");
    try {
      await call(`/admin/categories/${c.id}`, { method: "PATCH", body: { name } });
      notify("Category renamed.");
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  async function addCategory(e) {
    e.preventDefault();
    const name = freshCategory.trim();
    if (!name) return;
    setError("");
    try {
      await call("/admin/categories", { method: "POST", body: { name } });
      notify("Category added.");
      setFreshCategory("");
      await reload();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="max-w-6xl mx-auto w-full px-4 py-8 md:py-12">
      <h1 className="text-3xl font-bold text-text-main mb-8 tracking-tight">Admin Dashboard</h1>
      {error ? <div className="text-danger text-sm bg-danger/10 p-4 rounded-xl border border-danger/20 font-semibold mb-6">{error}</div> : null}
      
      <div className="grid grid-cols-1 lg:grid-cols-[360px_1fr] gap-8">
        <form className="bg-surface rounded-3xl p-6 md:p-8 h-fit border border-gray-100 shadow-[0_8px_30px_rgb(0,0,0,0.04)] lg:sticky lg:top-24" onSubmit={saveProduct}>
          <h2 className="text-2xl font-bold text-text-main mb-6">{editingId ? "Edit product" : "New product"}</h2>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">SKU</label>
            <input className="premium-input" value={form.sku} onChange={(e) => set("sku", e.target.value)} required disabled={Boolean(editingId)} />
          </div>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Name</label>
            <input className="premium-input" value={form.name} onChange={(e) => set("name", e.target.value)} required />
          </div>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Price (RWF)</label>
            <input className="premium-input" type="number" min="0" value={form.priceRwf} onChange={(e) => set("priceRwf", e.target.value)} />
          </div>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Stock</label>
            <input className="premium-input" type="number" min="0" value={form.stockQty} onChange={(e) => set("stockQty", e.target.value)} />
          </div>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Status</label>
            <select className="premium-input cursor-pointer" value={form.status} onChange={(e) => set("status", e.target.value)}>
              <option>PUBLISHED</option>
              <option>DRAFT</option>
            </select>
          </div>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1" htmlFor="admin-category">Category</label>
            <select className="premium-input cursor-pointer" id="admin-category" value={categoryChoice} onChange={(e) => setCategoryChoice(e.target.value)}>
              {categories.map((c) => (
                <option key={c.id} value={c.name}>{c.name}</option>
              ))}
              <option value={NEW_CATEGORY}>Add new category…</option>
            </select>
          </div>
          
          {categoryChoice === NEW_CATEGORY ? (
            <div className="flex flex-col gap-1.5 mb-4 p-4 bg-background rounded-xl border border-primary/20 shadow-[0_0_0_2px_rgba(0,120,212,0.1)]">
              <label className="text-xs font-bold text-primary uppercase tracking-wider ml-1" htmlFor="admin-new-category">New category</label>
              <input className="premium-input" id="admin-new-category" value={newCategory} onChange={(e) => setNewCategory(e.target.value)} placeholder="e.g. Tablets" required />
            </div>
          ) : null}
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1" htmlFor="admin-studio">Studio phone photo</label>
            <select
              className="premium-input cursor-pointer"
              id="admin-studio"
              value={STUDIO_PHONES.some((p) => p.url === form.imageUrl) ? form.imageUrl : ""}
              onChange={(e) => set("imageUrl", e.target.value)}
            >
              <option value="">Upload your own…</option>
              {STUDIO_PHONES.map((p) => (
                <option key={p.url} value={p.url}>{p.label}</option>
              ))}
            </select>
          </div>
          
          <div className="flex flex-col gap-1.5 mb-4">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1" htmlFor="admin-photo">Upload photo</label>
            <div className="relative overflow-hidden group">
              <button type="button" className="btn w-full bg-white border border-gray-200 text-text-main group-hover:bg-gray-50 flex items-center justify-center gap-2" onClick={() => document.getElementById('admin-photo').click()}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                Select Image
              </button>
              <input className="hidden" id="admin-photo" type="file" accept="image/*" onChange={onPhoto} />
            </div>
            <span className="text-[11px] text-text-muted ml-1">Resized to 720px, lightened, and saved as a small JPEG.</span>
          </div>
          
          {form.imageUrl ? <img className="w-full h-auto max-h-[200px] object-contain bg-white rounded-xl border border-gray-200 mb-4" src={form.imageUrl} alt="Product preview" /> : null}
          {compressing ? <p className="text-sm font-medium text-text-muted text-center animate-pulse mb-4">Optimising photo…</p> : null}
          
          <div className="flex flex-col gap-1.5 mb-6">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Description</label>
            <textarea className="premium-input resize-none" rows="3" value={form.description} onChange={(e) => set("description", e.target.value)} />
          </div>
          
          <button className="btn btn-primary w-full py-3 shadow-md hover:shadow-lg" type="submit" disabled={compressing}>
            {editingId ? "Save changes" : "Publish product"}
          </button>
          
          {editingId ? (
            <button className="btn btn-ghost w-full mt-3" type="button" onClick={cancelEdit}>
              Cancel edit
            </button>
          ) : null}
        </form>

        <div className="flex flex-col gap-10">
          <section className="glass-card p-6 md:p-8">
            <h2 className="text-2xl font-bold text-text-main mb-2 tracking-tight">Catalogue</h2>
            <p className="text-text-muted text-sm mb-6">Update stock inline, or open a row to edit name, category, and photo.</p>
            <div className="overflow-x-auto rounded-2xl border border-gray-200">
              <table className="w-full text-left text-sm whitespace-nowrap">
                <thead className="bg-surface/80 border-b border-gray-200 text-text-muted uppercase tracking-wider text-xs font-bold">
                  <tr>
                    <th className="px-4 py-3">SKU</th>
                    <th className="px-4 py-3">Name</th>
                    <th className="px-4 py-3">Category</th>
                    <th className="px-4 py-3">Stock</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {catalog.map((p) => (
                    <tr key={p.id} className="hover:bg-surface/50 transition-colors">
                      <td className="px-4 py-3 font-mono text-xs">{p.sku}</td>
                      <td className="px-4 py-3 font-medium text-text-main">{p.name}</td>
                      <td className="px-4 py-3 text-text-muted">{p.categoryName || "—"}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <input
                            className="w-16 text-center border border-gray-200 rounded-md p-1.5 bg-background focus:ring-2 focus:ring-primary/20 outline-none"
                            type="number"
                            min="0"
                            aria-label={`Stock for ${p.sku}`}
                            value={stockDrafts[p.id] ?? p.stockQty}
                            onChange={(e) => setStockDrafts((s) => ({ ...s, [p.id]: e.target.value }))}
                          />
                          <button className="btn bg-white border border-gray-200 px-3 py-1.5 text-xs font-medium" type="button" onClick={() => saveStock(p)}>Save</button>
                        </div>
                      </td>
                      <td className="px-4 py-3"><span className={`px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider rounded-full ${p.status === 'PUBLISHED' ? 'bg-success/10 text-success' : 'bg-gray-200 text-gray-600'}`}>{p.status}</span></td>
                      <td className="px-4 py-3"><button className="btn btn-ghost px-3 py-1.5 text-xs font-medium" type="button" onClick={() => startEdit(p)}>Edit</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="glass-card p-6 md:p-8">
            <h2 className="text-2xl font-bold text-text-main mb-6 tracking-tight">Categories</h2>
            <div className="flex flex-col gap-3">
              {categories.map((c) => (
                <div className="flex items-center gap-3 bg-surface p-3 rounded-xl border border-gray-100" key={c.id}>
                  <input
                    className="premium-input flex-1 bg-white"
                    value={categoryDrafts[c.id] ?? c.name}
                    onChange={(e) => setCategoryDrafts((d) => ({ ...d, [c.id]: e.target.value }))}
                    aria-label={`Rename ${c.name}`}
                  />
                  <span className="text-text-muted text-sm font-medium whitespace-nowrap w-24 text-right">{counts[c.id] || 0} products</span>
                  <button className="btn bg-white border border-gray-200 shadow-sm" type="button" onClick={() => saveCategory(c)}>Rename</button>
                </div>
              ))}
              <form className="flex items-center gap-3 bg-primary/5 p-3 rounded-xl border border-primary/20 mt-2" onSubmit={addCategory}>
                <input className="premium-input flex-1 bg-white" value={freshCategory} onChange={(e) => setFreshCategory(e.target.value)} placeholder="New category name" />
                <button className="btn btn-primary whitespace-nowrap" type="submit">Add category</button>
              </form>
            </div>
          </section>

          <section className="glass-card p-6 md:p-8">
            <h2 className="text-2xl font-bold text-text-main mb-6 tracking-tight">Fulfilment</h2>
            <div className="flex flex-col gap-4">
              {orders.map((o) => (
                <article className="bg-surface p-5 rounded-2xl border border-gray-100" key={o.id}>
                  <div className="flex items-center gap-2 mb-4">
                    <strong className="text-text-main">{o.orderNumber}</strong>
                    <span className="text-text-muted">·</span>
                    <span className={`px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-full ${['PAID', 'DELIVERED', 'SHIPPED'].includes(o.status) ? 'bg-success/10 text-success' : 'bg-primary/10 text-primary'}`}>
                      {o.status.replaceAll("_", " ")}
                    </span>
                    <span className="text-text-muted">·</span>
                    <span className="font-semibold text-text-main">{money(o.totalRwf)}</span>
                  </div>
                  <div className="flex flex-col sm:flex-row gap-3">
                    <input className="premium-input flex-1 bg-white" value={tracking} onChange={(e) => setTracking(e.target.value)} aria-label="Tracking" placeholder="Tracking code" />
                    <div className="flex gap-2">
                      <button className="btn bg-white border border-gray-200 shadow-sm flex-1 sm:flex-none" type="button" onClick={() => call(`/admin/orders/${o.id}/ship`, { method: "POST", body: { trackingNo: tracking } }).then(loadOrders).then(() => notify("Shipped")).catch((e) => setError(e.message))}>Ship</button>
                      <button className="btn btn-primary flex-1 sm:flex-none" type="button" onClick={() => call(`/admin/orders/${o.id}/deliver`, { method: "POST" }).then(loadOrders).then(() => notify("Delivered")).catch((e) => setError(e.message))}>Deliver</button>
                    </div>
                  </div>
                </article>
              ))}
              {orders.length === 0 && <p className="text-text-muted text-center py-6">No orders to fulfill.</p>}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
