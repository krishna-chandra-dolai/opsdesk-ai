"use client";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import AppShell from "@/components/AppShell";
import { api } from "@/lib/api";
import type { Incident, Level } from "@/lib/types";

export default function NewIncidentPage() {
  const router = useRouter(); const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  const [form, setForm] = useState<{title: string; description: string; impact: Level; urgency: Level}>({title: "", description: "", impact: "MEDIUM", urgency: "MEDIUM"});
  async function submit(event: FormEvent) { event.preventDefault(); setBusy(true); setError(""); try { const created = await api<Incident>("/api/incidents", {method: "POST", body: JSON.stringify(form)}); router.push(`/incidents/${created.id}`); } catch (reason) { setError(reason instanceof Error ? reason.message : "Could not create incident"); } finally { setBusy(false); } }
  return <AppShell><header className="pageHeader"><div><p className="eyebrow">Employee request</p><h2>Create incident</h2><p className="muted">Describe the problem clearly. Priority is calculated by the backend.</p></div></header><section className="panel formPanel"><form onSubmit={submit}>
    <label>Title<input value={form.title} maxLength={150} onChange={e => setForm({...form, title: e.target.value})} placeholder="Short summary of the issue" required /></label><label>Description<textarea value={form.description} maxLength={5000} rows={7} onChange={e => setForm({...form, description: e.target.value})} placeholder="What happened and how does it affect your work?" required /></label><div className="formGrid"><label>Impact<select value={form.impact} onChange={e => setForm({...form, impact: e.target.value as Level})}><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></label><label>Urgency<select value={form.urgency} onChange={e => setForm({...form, urgency: e.target.value as Level})}><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select></label></div>{error && <p className="error">{error}</p>}<div className="actions"><button type="button" className="secondary" onClick={() => router.back()}>Cancel</button><button disabled={busy}>{busy ? "Creating…" : "Create incident"}</button></div></form></section></AppShell>;
}
