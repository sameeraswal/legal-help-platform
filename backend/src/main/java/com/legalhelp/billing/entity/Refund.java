package com.legalhelp.billing.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(name = "pg_refund_ref", length = 128)
    private String pgRefundRef;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RefundStatus status = RefundStatus.INITIATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Refund() {
    }

    public Refund(Long paymentId, long amountMinorUnits, Long adminId) {
        this.paymentId = paymentId;
        this.amountMinorUnits = amountMinorUnits;
        this.adminId = adminId;
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public String getPgRefundRef() {
        return pgRefundRef;
    }

    public void setPgRefundRef(String pgRefundRef) {
        this.pgRefundRef = pgRefundRef;
    }

    public Long getAdminId() {
        return adminId;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
