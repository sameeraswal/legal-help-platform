import { apiFetch } from "./client";
import type { AdminUser, AppConfig, AuditLogEntry } from "./types/admin";
import type { Page } from "./types/billing";
import type { Role } from "./types/auth";

export function listUsersByRole(role: Role, page = 0): Promise<Page<AdminUser>> {
  return apiFetch<Page<AdminUser>>(`/api/admin/users?role=${role}&page=${page}`);
}

export function approveLawyer(userId: number): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${userId}/approve-lawyer`, { method: "POST" });
}

export function setUserStatus(userId: number, status: "ACTIVE" | "SUSPENDED"): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${userId}/status?status=${status}`, { method: "POST" });
}

export function getAppConfig(): Promise<AppConfig> {
  return apiFetch<AppConfig>("/api/admin/config");
}

export function updateGeneralConfig(freeMinutes: number, payoutThresholdMinorUnits: number): Promise<AppConfig> {
  return apiFetch<AppConfig>("/api/admin/config/general", {
    method: "PUT",
    body: JSON.stringify({ freeMinutes, payoutThresholdMinorUnits }),
  });
}

export function updatePaymentGatewayConfig(keyId: string, keySecret: string, webhookSecret: string): Promise<AppConfig> {
  return apiFetch<AppConfig>("/api/admin/config/payment-gateway", {
    method: "PUT",
    body: JSON.stringify({ keyId, keySecret, webhookSecret }),
  });
}

export function listAuditLogs(entity?: string, page = 0): Promise<Page<AuditLogEntry>> {
  const query = entity ? `?entity=${entity}&page=${page}` : `?page=${page}`;
  return apiFetch<Page<AuditLogEntry>>(`/api/admin/audit-logs${query}`);
}
