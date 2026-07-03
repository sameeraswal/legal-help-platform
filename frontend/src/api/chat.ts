import { apiFetch } from "./client";
import type { ChatMessage, ChatSession, OnlineLawyer } from "./types/chat";

export function listMySessions(): Promise<ChatSession[]> {
  return apiFetch<ChatSession[]>("/api/chat/sessions");
}

export function getActiveSession(): Promise<ChatSession | null> {
  return apiFetch<ChatSession | null>("/api/chat/sessions/active");
}

export function startLlmSession(): Promise<ChatSession> {
  return apiFetch<ChatSession>("/api/chat/sessions/llm", { method: "POST" });
}

export function startLawyerSession(lawyerId: number): Promise<ChatSession> {
  return apiFetch<ChatSession>("/api/chat/sessions/lawyer", { method: "POST", body: JSON.stringify({ lawyerId }) });
}

export function endSession(sessionId: number): Promise<void> {
  return apiFetch<void>(`/api/chat/sessions/${sessionId}/end`, { method: "POST" });
}

export function getSessionMessages(sessionId: number): Promise<ChatMessage[]> {
  return apiFetch<ChatMessage[]>(`/api/chat/sessions/${sessionId}/messages`);
}

export function listOnlineLawyers(): Promise<OnlineLawyer[]> {
  return apiFetch<OnlineLawyer[]>("/api/lawyers/online");
}

export function listMyLawyerSessions(): Promise<ChatSession[]> {
  return apiFetch<ChatSession[]>("/api/lawyer/chat/sessions");
}

export function getLawyerActiveSession(): Promise<ChatSession | null> {
  return apiFetch<ChatSession | null>("/api/lawyer/chat/sessions/active");
}

export function getLawyerSessionMessages(sessionId: number): Promise<ChatMessage[]> {
  return apiFetch<ChatMessage[]>(`/api/lawyer/chat/sessions/${sessionId}/messages`);
}

export function endLawyerSession(sessionId: number): Promise<void> {
  return apiFetch<void>(`/api/lawyer/chat/sessions/${sessionId}/end`, { method: "POST" });
}

export function goOnline(): Promise<void> {
  return apiFetch<void>("/api/lawyer/chat/presence/online", { method: "POST" });
}

export function goOffline(): Promise<void> {
  return apiFetch<void>("/api/lawyer/chat/presence/offline", { method: "POST" });
}
