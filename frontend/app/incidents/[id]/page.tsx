"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import AppShell from "@/components/AppShell";
import { api, formatDate } from "@/lib/api";
import { useSession } from "@/lib/auth";
import type { Activity, Comment, Incident, Page, Status, User } from "@/lib/types";

export default function IncidentDetailPage() {
  const id = useParams<{id: string}>().id;
  const role = useSession()?.user.role;
  const [incident, setIncident] = useState<Incident | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [activity, setActivity] = useState<Activity[]>([]);
  const [engineers, setEngineers] = useState<User[]>([]);
  const [assigneeId, setAssigneeId] = useState("");
  const [comment, setComment] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const [ticket, notes, history] = await Promise.all([
        api<Incident>(`/api/incidents/${id}`),
        api<Comment[]>(`/api/incidents/${id}/comments`),
        api<Activity[]>(`/api/incidents/${id}/activity`),
      ]);
      setIncident(ticket); setComments(notes); setActivity(history); setError("");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Could not load incident"); }
  }, [id]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);
  useEffect(() => {
    if (role === "ADMIN") api<Page<User>>("/api/users?size=100")
      .then(page => setEngineers(page.content.filter(user => user.role === "SUPPORT_ENGINEER")))
      .catch(reason => setError(reason.message));
  }, [role]);

  async function action(path: string, body: object) {
    setBusy(true); setError("");
    try { await api(`/api/incidents/${id}/${path}`, {method: "PATCH", body: JSON.stringify(body)}); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Update failed"); } finally { setBusy(false); }
  }
  async function setStatus(status: Status) { await action("status", {status}); }
  async function addComment(event: FormEvent) {
    event.preventDefault(); if (!comment.trim()) return; setBusy(true);
    try { await api(`/api/incidents/${id}/comments`, {method: "POST", body: JSON.stringify({content: comment})}); setComment(""); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Comment failed"); } finally { setBusy(false); }
  }

  return <AppShell>
    {!incident ? <section className="panel"><p>{error || "Loading incident…"}</p></section> : <>
      <header className="pageHeader"><div><p className="eyebrow">Incident #{incident.id}</p><h2>{incident.title}</h2><div className="inlineMeta"><span className={`badge ${incident.priority.toLowerCase()}`}>{incident.priority}</span><span className="status">{incident.status.replaceAll("_", " ")}</span>{incident.slaBreached && <span className="breach">SLA breached</span>}</div></div>
        <div className="actions">
          {role === "SUPPORT_ENGINEER" && ["OPEN", "REOPENED"].includes(incident.status) && <button disabled={busy} onClick={() => action("assign", {})}>Claim incident</button>}
          {role === "SUPPORT_ENGINEER" && incident.status === "ASSIGNED" && <button disabled={busy} onClick={() => setStatus("IN_PROGRESS")}>Start progress</button>}
          {role === "SUPPORT_ENGINEER" && incident.status === "IN_PROGRESS" && <button disabled={busy} onClick={() => setStatus("RESOLVED")}>Resolve</button>}
          {role === "EMPLOYEE" && incident.status === "RESOLVED" && <><button className="secondary" disabled={busy} onClick={() => setStatus("REOPENED")}>Reopen</button><button disabled={busy} onClick={() => setStatus("CLOSED")}>Close incident</button></>}
        </div>
      </header>
      {error && <p className="error panel">{error}</p>}
      {role === "ADMIN" && !["RESOLVED", "CLOSED"].includes(incident.status) && <section className="panel assignBar"><label>Assign support engineer<select value={assigneeId} onChange={e => setAssigneeId(e.target.value)}><option value="">Select engineer</option>{engineers.map(user => <option key={user.id} value={user.id}>{user.name}</option>)}</select></label><button disabled={!assigneeId || busy} onClick={() => action("assign", {assigneeId: Number(assigneeId)})}>Assign</button></section>}
      <div className="detailGrid"><section className="panel"><h3>Description</h3><p className="description">{incident.description}</p><dl className="facts"><div><dt>Impact</dt><dd>{incident.impact}</dd></div><div><dt>Urgency</dt><dd>{incident.urgency}</dd></div><div><dt>Category</dt><dd>{incident.category}</dd></div><div><dt>AI suggestion</dt><dd>{incident.aiSuggestedCategory ?? "Unavailable"}{incident.aiConfidence !== null ? ` (${Math.round(incident.aiConfidence * 100)}%)` : ""}</dd></div><div><dt>Reporter</dt><dd>{incident.reporter.name}</dd></div><div><dt>Assignee</dt><dd>{incident.assignee?.name ?? "Unassigned"}</dd></div><div><dt>Created</dt><dd>{formatDate(incident.createdAt)}</dd></div><div><dt>SLA deadline</dt><dd>{formatDate(incident.slaDeadline)}</dd></div></dl></section>
        <section className="panel"><h3>Activity</h3><div className="timeline">{activity.map(item => <article key={item.id}><span className="dot"/><div><strong>{item.action.replaceAll("_", " ")}</strong><p>{item.actor?.name ?? "System"} · {formatDate(item.createdAt)}</p>{item.oldValue || item.newValue ? <small>{item.oldValue ?? "—"} → {item.newValue ?? "—"}</small> : null}</div></article>)}</div></section></div>
      <section className="panel comments"><h3>Comments</h3>{comments.map(item => <article key={item.id}><div><strong>{item.author.name}</strong><span>{formatDate(item.createdAt)}</span></div><p>{item.content}</p></article>)}{comments.length === 0 && <p className="muted">No comments yet.</p>}<form onSubmit={addComment}><textarea value={comment} onChange={e => setComment(e.target.value)} maxLength={2000} rows={3} placeholder="Add a useful update" required/><button disabled={busy}>Add comment</button></form></section>
    </>}
  </AppShell>;
}
