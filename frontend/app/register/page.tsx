"use client";
import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";

export default function RegisterPage() {
  const [form, setForm] = useState({name: "", email: "", password: ""}); const [error, setError] = useState(""); const router = useRouter();
  async function submit(event: FormEvent) { event.preventDefault(); setError(""); try { await api("/api/auth/register", {method: "POST", body: JSON.stringify(form)}); router.push("/login"); } catch (reason) { setError(reason instanceof Error ? reason.message : "Registration failed"); } }
  return <main className="authPage"><section className="authCard"><div className="brand">OD</div><h1>Create employee account</h1><p className="muted">Public registration always creates an employee role.</p><form onSubmit={submit}>
    <label>Name<input value={form.name} onChange={e => setForm({...form, name: e.target.value})} maxLength={100} required /></label><label>Email<input type="email" value={form.email} onChange={e => setForm({...form, email: e.target.value})} required /></label><label>Password<input type="password" minLength={8} maxLength={72} value={form.password} onChange={e => setForm({...form, password: e.target.value})} required /></label>{error && <p className="error">{error}</p>}<button>Create account</button></form><p className="authLink"><Link href="/login">Back to sign in</Link></p></section></main>;
}
