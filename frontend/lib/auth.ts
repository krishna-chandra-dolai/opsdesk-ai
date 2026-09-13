"use client";
import { useMemo, useSyncExternalStore } from "react";
import type { Session } from "./types";

const KEY = "opsdesk-session";
const listeners = new Set<() => void>();

function snapshot() { return typeof window === "undefined" ? null : window.localStorage.getItem(KEY); }
function subscribe(listener: () => void) {
  listeners.add(listener);
  const onStorage = () => listener();
  window.addEventListener("storage", onStorage);
  return () => { listeners.delete(listener); window.removeEventListener("storage", onStorage); };
}
function notify() { listeners.forEach(listener => listener()); }

export function loadSession(): Session | null {
  if (typeof window === "undefined") return null;
  const stored = window.localStorage.getItem(KEY);
  if (!stored) return null;
  try { return JSON.parse(stored) as Session; } catch { clearSession(); return null; }
}

export function saveSession(session: Session) { window.localStorage.setItem(KEY, JSON.stringify(session)); notify(); }
export function clearSession() { if (typeof window !== "undefined") { window.localStorage.removeItem(KEY); notify(); } }

export function useSession(): Session | null {
  const stored = useSyncExternalStore(subscribe, snapshot, () => null);
  return useMemo(() => {
    if (!stored) return null;
    try { return JSON.parse(stored) as Session; } catch { return null; }
  }, [stored]);
}
