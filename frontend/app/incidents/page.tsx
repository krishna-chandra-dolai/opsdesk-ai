"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import AppShell from "@/components/AppShell";
import { api, formatDate } from "@/lib/api";
import { useSession } from "@/lib/auth";
import type { Incident, Page } from "@/lib/types";

export default function IncidentsPage() {
  const [data, setData] = useState<Page<Incident> | null>(null); const [error, setError] = useState("");
  useEffect(() => { api<Page<Incident>>("/api/incidents?size=25&sort=createdAt,desc").then(setData).catch(e => setError(e.message)); }, []);
  const role = useSession()?.user.role;
  return <AppShell><header className="pageHeader"><div><p className="eyebrow">{role === "EMPLOYEE" ? "My work" : "Support queue"}</p><h2>{role === "EMPLOYEE" ? "My incidents" : "Incident queue"}</h2><p className="muted">{data ? `${data.page.totalElements} incident${data.page.totalElements === 1 ? "" : "s"}` : "Loading…"}</p></div>{role === "EMPLOYEE" && <Link className="button" href="/incidents/new">Create incident</Link>}</header>
    {error && <p className="error panel">{error}</p>}<section className="panel tableWrap"><table><thead><tr><th>Incident</th><th>Priority</th><th>Status</th><th>Assignee</th><th>Created</th></tr></thead><tbody>{data?.content.map(item => <tr key={item.id}><td><Link className="titleLink" href={`/incidents/${item.id}`}>#{item.id} · {item.title}</Link><span className="cellSub">{item.category}</span></td><td><span className={`badge ${item.priority.toLowerCase()}`}>{item.priority}</span></td><td><span className="status">{item.status.replaceAll("_", " ")}</span></td><td>{item.assignee?.name ?? "Unassigned"}</td><td>{formatDate(item.createdAt)}</td></tr>)}</tbody></table>{data?.content.length === 0 && <div className="empty"><h3>No incidents yet</h3><p>Create one when you need IT help.</p></div>}</section>
  </AppShell>;
}
