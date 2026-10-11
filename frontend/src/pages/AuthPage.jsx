import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Mark } from "../shell";
import { useStore } from "../store";

function Eye({ off = false }) {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
      {off ? (
        <>
          <path d="M3 3l18 18" />
          <path d="M10.6 10.7a2 2 0 0 0 2.7 2.7" />
          <path d="M9.9 5.2A11 11 0 0 1 12 5c5 0 9.3 3.1 11 7-.6 1.3-1.5 2.5-2.6 3.5" />
          <path d="M6.6 6.6C4.6 7.9 3 9.8 1 12c1.7 3.9 6 7 11 7 1.6 0 3.1-.3 4.5-.9" />
        </>
      ) : (
        <>
          <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z" />
          <circle cx="12" cy="12" r="3" />
        </>
      )}
    </svg>
  );
}

export function AuthPage({ mode }) {
  const { login, register } = useStore();
  const navigate = useNavigate();
  const [email, setEmail] = useState(mode === "login" ? "demo@marketsphere.rw" : "");
  const [password, setPassword] = useState(mode === "login" ? "MarketSphere1" : "");
  const [phone, setPhone] = useState("0780000001");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");

  async function onSubmit(e) {
    e.preventDefault();
    setError("");
    try {
      if (mode === "login") await login(email, password);
      else await register({ email, password, phone });
      navigate("/");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden bg-background">
      <div className="absolute top-[-10%] left-[-10%] w-[500px] h-[500px] rounded-full bg-primary/10 blur-[100px] pointer-events-none"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[500px] h-[500px] rounded-full bg-secondary/10 blur-[100px] pointer-events-none"></div>
      
      <form className="glass-card w-full max-w-md p-8 md:p-10 shadow-2xl relative z-10" onSubmit={onSubmit}>
        <div className="flex justify-center mb-6"><Mark /></div>
        <h1 className="text-3xl font-bold text-center text-text-main mb-2 tracking-tight">{mode === "login" ? "Welcome back" : "Create account"}</h1>
        <p className="text-center text-text-muted mb-8 font-medium">Market Sphere ecommerce</p>
        
        <div className="flex flex-col gap-1.5 mb-5">
          <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Email</label>
          <input className="premium-input" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" placeholder="name@example.com" />
        </div>
        
        <div className="flex flex-col gap-1.5 mb-6">
          <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1" htmlFor="auth-password">Password</label>
          <div className="relative">
            <input
              id="auth-password"
              className="premium-input pr-12"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              placeholder="••••••••"
            />
            <button
              type="button"
              className="absolute right-2 top-1/2 -translate-y-1/2 w-9 h-9 rounded-lg flex items-center justify-center text-text-muted hover:bg-surface hover:text-primary transition-colors"
              aria-label={showPassword ? "Hide password" : "Show password"}
              aria-pressed={showPassword}
              onClick={() => setShowPassword((open) => !open)}
            >
              <Eye off={showPassword} />
            </button>
          </div>
        </div>
        
        {mode === "register" ? (
          <div className="flex flex-col gap-1.5 mb-6">
            <label className="text-xs font-bold text-text-muted uppercase tracking-wider ml-1">Mobile</label>
            <input className="premium-input" value={phone} onChange={(e) => setPhone(e.target.value)} required pattern="^07[0-9]{8}$" placeholder="07xxxxxxxx" />
          </div>
        ) : null}
        
        {error ? <div className="bg-danger/10 text-danger text-sm font-semibold p-3 rounded-xl border border-danger/20 mb-6">{error}</div> : null}
        
        <button className="btn btn-primary w-full py-4 text-lg shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30 mt-2" type="submit">
          {mode === "login" ? "Sign in" : "Create account"}
        </button>
        
        <p className="text-center mt-8 text-text-muted font-medium text-sm">
          {mode === "login" ? (
            <>Need an account? <Link to="/register" className="text-primary hover:underline ml-1">Register</Link></>
          ) : (
            <>Already a customer? <Link to="/login" className="text-primary hover:underline ml-1">Sign in</Link></>
          )}
        </p>
      </form>
    </div>
  );
}
