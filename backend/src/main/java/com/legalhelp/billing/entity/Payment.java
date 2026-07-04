package com.legalhelp.billing.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * The original payment row is never mutated by a refund (CLAUDE.md domain rule #7) —
 * refunds are a separate {@link Refund} row plus a compensating ledger entry.
 * `rawWebhookPayload` stores the webhook body verbatim for reconciliation/audit.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 128)
    private String orderId;

    /** Razorpay order id, returned by createOrder — distinct from pgRef (the payment id, set on success). */
    @Column(name = "pg_order_id", unique = true, length = 128)
    private String pgOrderId;

    @Column(name = "pg_ref", unique = true, length = 128)
    private String pgRef;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "raw_webhook_payload", columnDefinition = "LONGTEXT")
    private String rawWebhookPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Payment() {
    }

    public Payment(String orderId, Long customerId, Long planId, long amountMinorUnits) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.planId = planId;
        this.amountMinorUnits = amountMinorUnits;
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPgOrderId() {
        return pgOrderId;
    }

    public void setPgOrderId(String pgOrderId) {
        this.pgOrderId = pgOrderId;
    }

    public String getPgRef() {
        return pgRef;
    }

    public void setPgRef(String pgRef) {
        this.pgRef = pgRef;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getPlanId() {
        return planId;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getRawWebhookPayload() {
        return rawWebhookPayload;
    }

    public void setRawWebhookPayload(String rawWebhookPayload) {
        this.rawWebhookPayload = rawWebhookPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
