package com.legalhelp.billing.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payout_requests")
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lawyer_id", nullable = false)
    private Long lawyerId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "bank_reference", length = 128)
    private String bankReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected PayoutRequest() {
    }

    public PayoutRequest(Long lawyerId, long amountMinorUnits) {
        this.lawyerId = lawyerId;
        this.amountMinorUnits = amountMinorUnits;
    }

    public Long getId() {
        return id;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
        this.decidedAt = Instant.now();
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
