package com.legalhelp.billing.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Append-only, double-entry-style ledger. No update/delete methods exist anywhere
 * in this codebase for this entity (CLAUDE.md domain rule #2) — every balance
 * change is a new row carrying the resulting balance.
 */
@Entity
@Table(name = "wallet_ledger")
public class WalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 16)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private LedgerEntryType entryType;

    @Column(name = "seconds_delta")
    private Long secondsDelta;

    @Column(name = "amount_delta_minor_units")
    private Long amountDeltaMinorUnits;

    @Column(name = "balance_after_seconds")
    private Long balanceAfterSeconds;

    @Column(name = "balance_after_amount_minor_units")
    private Long balanceAfterAmountMinorUnits;

    @Column(length = 128)
    private String reference;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WalletLedger() {
    }

    public static WalletLedger customerEntry(Long customerId, LedgerEntryType entryType, long secondsDelta,
                                              long balanceAfterSeconds, String reference) {
        WalletLedger entry = new WalletLedger();
        entry.userId = customerId;
        entry.walletType = WalletType.CUSTOMER;
        entry.entryType = entryType;
        entry.secondsDelta = secondsDelta;
        entry.balanceAfterSeconds = balanceAfterSeconds;
        entry.reference = reference;
        return entry;
    }

    public static WalletLedger lawyerEntry(Long lawyerId, LedgerEntryType entryType, long amountDeltaMinorUnits,
                                            long balanceAfterAmountMinorUnits, String reference) {
        WalletLedger entry = new WalletLedger();
        entry.userId = lawyerId;
        entry.walletType = WalletType.LAWYER;
        entry.entryType = entryType;
        entry.amountDeltaMinorUnits = amountDeltaMinorUnits;
        entry.balanceAfterAmountMinorUnits = balanceAfterAmountMinorUnits;
        entry.reference = reference;
        return entry;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public Long getSecondsDelta() {
        return secondsDelta;
    }

    public Long getAmountDeltaMinorUnits() {
        return amountDeltaMinorUnits;
    }

    public Long getBalanceAfterSeconds() {
        return balanceAfterSeconds;
    }

    public Long getBalanceAfterAmountMinorUnits() {
        return balanceAfterAmountMinorUnits;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
