package com.legalhelp.billing.entity;

import jakarta.persistence.*;

/**
 * Row-locked cache of the customer's time balance. The {@link WalletLedger} is the
 * source of truth (CLAUDE.md domain rule #2) — every mutation here happens in the
 * same transaction as, and must always reconcile to, the sum of that customer's
 * ledger rows. See WalletService for the locking discipline.
 */
@Entity
@Table(name = "customer_wallets")
public class CustomerWallet {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "remaining_seconds", nullable = false)
    private long remainingSeconds;

    @Column(name = "free_seconds_remaining", nullable = false)
    private long freeSecondsRemaining;

    protected CustomerWallet() {
    }

    public CustomerWallet(Long customerId, long freeSecondsRemaining) {
        this.customerId = customerId;
        this.freeSecondsRemaining = freeSecondsRemaining;
        this.remainingSeconds = 0;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public long getFreeSecondsRemaining() {
        return freeSecondsRemaining;
    }

    public void setFreeSecondsRemaining(long freeSecondsRemaining) {
        this.freeSecondsRemaining = freeSecondsRemaining;
    }

    public long totalAvailableSeconds() {
        return freeSecondsRemaining + remainingSeconds;
    }
}
