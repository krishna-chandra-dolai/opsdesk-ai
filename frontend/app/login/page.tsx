"use client";
import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { saveSession } from "@/lib/auth";
import type { Session } from "@/lib/types";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const router = useRouter();
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError("");
    try { const session = await api<Session>("/api/auth/login", {method: "POST", body: JSON.stringify({email, password})}); saveSession(session); router.replace(session.user.role === "ADMIN" ? "/admin" : "/incidents"); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Login failed"); } finally { setBusy(false); }
  }
  return <main className="authPage"><section className="authCard"><div className="brand">OD</div><h1>Welcome to OpsDesk</h1><p className="muted">Sign in to manage IT incidents.</p>
    <form onSubmit={submit}><label>Email<input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label><label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>{error && <p className="error">{error}</p>}<button disabled={busy}>{busy ? "Signing in…" : "Sign in"}</button></form>
    <p className="authLink">New employee? <Link href="/register">Create an account</Link></p></section></main>;
}
