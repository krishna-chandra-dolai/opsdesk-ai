export type Role = "EMPLOYEE" | "SUPPORT_ENGINEER" | "ADMIN";
export type Level = "LOW" | "MEDIUM" | "HIGH";
export type Priority = Level | "CRITICAL";
export type Status = "OPEN" | "ASSIGNED" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | "REOPENED";

export interface User { id: number; name: string; email: string; role: Role; createdAt: string }
export interface Session { token: string; tokenType: string; expiresIn: number; user: User }
export interface Incident {
  id: number; title: string; description: string; impact: Level; urgency: Level;
  priority: Priority; status: Status; category: string; aiSuggestedCategory: string | null;
  aiConfidence: number | null; reporter: User; assignee: User | null; createdAt: string;
  updatedAt: string; resolvedAt: string | null; slaDeadline: string | null; slaBreached: boolean;
}
export interface Page<T> { content: T[]; page: { size: number; number: number; totalElements: number; totalPages: number } }
export interface Comment { id: number; author: User; content: string; createdAt: string }
export interface Activity { id: number; actor: User | null; action: string; oldValue: string | null; newValue: string | null; createdAt: string }
export interface Summary { totalIncidents: number; openIncidents: number; inProgressIncidents: number; resolvedIncidents: number; slaBreachedIncidents: number; criticalIncidents: number }
