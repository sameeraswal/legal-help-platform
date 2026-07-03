export interface Plan {
  id: number;
  name: string;
  priceMinorUnits: number;
  seconds: number;
  active: boolean;
}

export interface PlanUpsertRequest {
  name: string;
  priceMinorUnits: number;
  seconds: number;
  active: boolean;
}

export interface WalletBalance {
  freeSecondsRemaining: number;
  paidSecondsRemaining: number;
  totalSecondsAvailable: number;
}

export type LedgerEntryType = "RECHARGE" | "CONSUME" | "REFUND" | "EARNING" | "PAYOUT";

export interface LedgerEntry {
  id: number;
  entryType: LedgerEntryType;
  secondsDelta: number | null;
  amountDeltaMinorUnits: number | null;
  reference: string | null;
  createdAt: string;
}

export interface PurchaseInitiateResponse {
  orderId: string;
  pgOrderId: string;
  amountMinorUnits: number;
  currency: string;
  pgKeyId: string;
}

export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "REFUNDED";

export interface Payment {
  id: number;
  orderId: string;
  customerId: number;
  planId: number;
  amountMinorUnits: number;
  status: PaymentStatus;
  createdAt: string;
}

export interface Refund {
  id: number;
  paymentId: number;
  amountMinorUnits: number;
  status: "INITIATED" | "SUCCESS" | "FAILED";
  createdAt: string;
}

export interface LawyerRate {
  id: number;
  lawyerId: number | null;
  perMinuteRateMinorUnits: number;
  effectiveFrom: string;
}

export type PayoutStatus = "PENDING" | "APPROVED" | "REJECTED" | "PAID";

export interface PayoutRequest {
  id: number;
  lawyerId: number;
  amountMinorUnits: number;
  status: PayoutStatus;
  bankReference: string | null;
  createdAt: string;
  decidedAt: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}
