export type CounterpartType = "LLM" | "LAWYER";
export type ChatSessionStatus = "ACTIVE" | "ENDED";
export type MessageSender = "CUSTOMER" | "LAWYER" | "LLM" | "SYSTEM";

export interface ChatSession {
  id: number;
  customerId: number;
  counterpartType: CounterpartType;
  lawyerId: number | null;
  status: ChatSessionStatus;
  billedSeconds: number;
  startedAt: string;
  endedAt: string | null;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  sender: MessageSender;
  content: string;
  timestamp: string;
}

export interface OnlineLawyer {
  lawyerId: number;
  name: string;
}

export type OutgoingEventType =
  | "MESSAGE"
  | "TOKEN_DELTA"
  | "TOKEN_COMPLETE"
  | "SESSION_WARNING"
  | "SESSION_ENDED"
  | "PRESENCE_UPDATE"
  | "ERROR";

export interface ChatEvent {
  type: OutgoingEventType;
  payload: unknown;
}
