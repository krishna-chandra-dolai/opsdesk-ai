"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { clearSession, useSession } from "@/lib/auth";

export default function AppShell({children}: {children: React.ReactNode}) {
  const session = useSession();
  const pathname = usePathname();
  const router = useRouter();
  useEffect(() => {
    if (!session) router.replace("/login");
  }, [router, session]);
  if (!session) return <main className="center"><p>Loading OpsDesk…</p></main>;
  const logout = () => { clearSession(); router.replace("/login"); };
  return <div className="app">
    <aside>
      <div><h1>OpsDesk</h1><p className="muted">IT Service Desk</p></div>
      <nav>
        <Link className={pathname === "/incidents" ? "active" : ""} href="/incidents">Incidents</Link>
        {session.user.role === "EMPLOYEE" && <Link className={pathname === "/incidents/new" ? "active" : ""} href="/incidents/new">Create incident</Link>}
        {session.user.role === "ADMIN" && <Link className={pathname === "/admin" ? "active" : ""} href="/admin">Dashboard</Link>}
      </nav>
      <div className="account"><strong>{session.user.name}</strong><span>{session.user.role.replaceAll("_", " ")}</span><button className="linkButton" onClick={logout}>Sign out</button></div>
    </aside>
    <main>{children}</main>
  </div>;
}
