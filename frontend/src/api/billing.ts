import { apiFetch } from "./client";
import type {
  LawyerRate,
  LedgerEntry,
  Page,
  Payment,
  PaymentStatus,
  PayoutRequest,
  Plan,
  PlanUpsertRequest,
  PurchaseInitiateResponse,
  Refund,
  WalletBalance,
} from "./types/billing";

export function listPlans(): Promise<Plan[]> {
  return apiFetch<Plan[]>("/api/plans");
}

export function listAllPlansAdmin(): Promise<Plan[]> {
  return apiFetch<Plan[]>("/api/admin/plans");
}

export function createPlan(request: PlanUpsertRequest): Promise<Plan> {
  return apiFetch<Plan>("/api/admin/plans", { method: "POST", body: JSON.stringify(request) });
}

export function updatePlan(id: number, request: PlanUpsertRequest): Promise<Plan> {
  return apiFetch<Plan>(`/api/admin/plans/${id}`, { method: "PUT", body: JSON.stringify(request) });
}

export function initiatePurchase(planId: number): Promise<PurchaseInitiateResponse> {
  return apiFetch<PurchaseInitiateResponse>(`/api/purchases/plans/${planId}`, { method: "POST" });
}

export function getWalletBalance(): Promise<WalletBalance> {
  return apiFetch<WalletBalance>("/api/wallet/balance");
}

export function getWalletTransactions(page = 0): Promise<Page<LedgerEntry>> {
  return apiFetch<Page<LedgerEntry>>(`/api/wallet/transactions?page=${page}`);
}

export function getLawyerWalletBalance(): Promise<WalletBalance> {
  return apiFetch<WalletBalance>("/api/lawyer/wallet/balance");
}

export function requestPayout(): Promise<PayoutRequest> {
  return apiFetch<PayoutRequest>("/api/lawyer/wallet/payout-requests", { method: "POST" });
}

export function listMyPayoutRequests(page = 0): Promise<Page<PayoutRequest>> {
  return apiFetch<Page<PayoutRequest>>(`/api/lawyer/wallet/payout-requests?page=${page}`);
}

export function listTransactionsAdmin(status?: PaymentStatus, page = 0): Promise<Page<Payment>> {
  const query = status ? `?status=${status}&page=${page}` : `?page=${page}`;
  return apiFetch<Page<Payment>>(`/api/admin/transactions${query}`);
}

export function refundPayment(paymentId: number): Promise<Refund> {
  return apiFetch<Refund>(`/api/admin/payments/${paymentId}/refund`, { method: "POST" });
}

export function setLawyerRate(lawyerId: number | null, perMinuteRateMinorUnits: number): Promise<LawyerRate> {
  return apiFetch<LawyerRate>("/api/admin/lawyer-rates", {
    method: "POST",
    body: JSON.stringify({ lawyerId, perMinuteRateMinorUnits }),
  });
}

export function getGlobalRateHistory(): Promise<LawyerRate[]> {
  return apiFetch<LawyerRate[]>("/api/admin/lawyer-rates/global");
}

export function listPendingPayouts(page = 0): Promise<Page<PayoutRequest>> {
  return apiFetch<Page<PayoutRequest>>(`/api/admin/payout-requests?page=${page}`);
}

export function decidePayout(id: number, approve: boolean, bankReference?: string): Promise<PayoutRequest> {
  return apiFetch<PayoutRequest>(`/api/admin/payout-requests/${id}/decision`, {
    method: "POST",
    body: JSON.stringify({ approve, bankReference }),
  });
}
