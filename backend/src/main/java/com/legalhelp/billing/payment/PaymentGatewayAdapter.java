package com.legalhelp.billing.payment;

/**
 * Adapter boundary between billing and the payment gateway (CLAUDE.md: "PG can be
 * switched/configured from admin without code change"). Swapping providers means
 * adding a new implementation here — never call a gateway SDK/API directly from a
 * billing service.
 */
public interface PaymentGatewayAdapter {

    PgOrder createOrder(long amountMinorUnits, String receiptId);

    PgRefundResult refund(String pgPaymentRef, long amountMinorUnits);

    /** Verifies a webhook delivery's signature against the raw request body. */
    boolean verifyWebhookSignature(String rawBody, String signatureHeader);
}
