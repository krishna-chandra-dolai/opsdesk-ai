"use client";
import { useEffect, useState } from "react";
import AppShell from "@/components/AppShell";
import { api } from "@/lib/api";
import type { Summary } from "@/lib/types";

export default function AdminPage() {
  const [summary, setSummary] = useState<Summary | null>(null); const [error, setError] = useState("");
  useEffect(() => { api<Summary>("/api/admin/summary").then(setSummary).catch(e => setError(e.message)); }, []);
  const metrics = summary ? [["Total", summary.totalIncidents], ["Open", summary.openIncidents], ["In progress", summary.inProgressIncidents], ["Resolved", summary.resolvedIncidents], ["SLA breached", summary.slaBreachedIncidents], ["Critical", summary.criticalIncidents]] : [];
  return <AppShell><header className="pageHeader"><div><p className="eyebrow">Operations</p><h2>Service desk summary</h2><p className="muted">Current values calculated from the incident database.</p></div></header>{error && <p className="error panel">{error}</p>}<section className="metrics">{metrics.map(([label, value]) => <article className="metric" key={label}><span>{label}</span><strong>{value}</strong></article>)}</section></AppShell>;
}
