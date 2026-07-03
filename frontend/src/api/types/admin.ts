import type { Role } from "./auth";

export type UserStatus = "ACTIVE" | "SUSPENDED";

export interface AdminUser {
  id: number;
  role: Role;
  name: string;
  email: string;
  phone: string | null;
  status: UserStatus;
  lawyerApproved: boolean | null;
  createdAt: string;
}

export interface AppConfig {
  freeMinutes: number;
  payoutThresholdMinorUnits: number;
  pgConfigured: boolean;
}

export interface AuditLogEntry {
  id: number;
  actorId: number | null;
  actorRole: string | null;
  action: string;
  entity: string;
  entityId: string | null;
  beforeState: string | null;
  afterState: string | null;
  createdAt: string;
}
