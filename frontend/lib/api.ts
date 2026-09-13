import { clearSession, loadSession } from "./auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const session = loadSession();
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (session?.token && !path.startsWith("/api/auth/")) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }
  const response = await fetch(`${API_URL}${path}`, {...options, headers, cache: "no-store"});
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    if (response.status === 401 && !path.startsWith("/api/auth/")) {
      clearSession();
    }
    throw new ApiError(response.status, body.message ?? `Request failed (${response.status})`);
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export function formatDate(value: string | null) {
  return value ? new Intl.DateTimeFormat(undefined, {dateStyle: "medium", timeStyle: "short"}).format(new Date(value)) : "—";
}
