package com.legalhelp.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Single-row table (id is always 1). PG credential fields are stored encrypted
 * (AES-256-GCM via EncryptionService) — never persisted or logged in plaintext.
 * When a PG field is null, RazorpayAdapter falls back to the env-var default so
 * the platform is usable before an admin has visited the config screen.
 */
@Entity
@Table(name = "app_config")
public class AppConfig {

    @Id
    private Long id = 1L;

    @Column(name = "free_minutes", nullable = false)
    private int freeMinutes;

    @Column(name = "payout_threshold_minor_units", nullable = false)
    private long payoutThresholdMinorUnits;

    @Column(name = "pg_key_id_encrypted", columnDefinition = "TEXT")
    private String pgKeyIdEncrypted;

    @Column(name = "pg_key_secret_encrypted", columnDefinition = "TEXT")
    private String pgKeySecretEncrypted;

    @Column(name = "pg_webhook_secret_encrypted", columnDefinition = "TEXT")
    private String pgWebhookSecretEncrypted;

    protected AppConfig() {
    }

    public AppConfig(int freeMinutes, long payoutThresholdMinorUnits) {
        this.freeMinutes = freeMinutes;
        this.payoutThresholdMinorUnits = payoutThresholdMinorUnits;
    }

    public Long getId() {
        return id;
    }

    public int getFreeMinutes() {
        return freeMinutes;
    }

    public void setFreeMinutes(int freeMinutes) {
        this.freeMinutes = freeMinutes;
    }

    public long getPayoutThresholdMinorUnits() {
        return payoutThresholdMinorUnits;
    }

    public void setPayoutThresholdMinorUnits(long payoutThresholdMinorUnits) {
        this.payoutThresholdMinorUnits = payoutThresholdMinorUnits;
    }

    public String getPgKeyIdEncrypted() {
        return pgKeyIdEncrypted;
    }

    public void setPgKeyIdEncrypted(String pgKeyIdEncrypted) {
        this.pgKeyIdEncrypted = pgKeyIdEncrypted;
    }

    public String getPgKeySecretEncrypted() {
        return pgKeySecretEncrypted;
    }

    public void setPgKeySecretEncrypted(String pgKeySecretEncrypted) {
        this.pgKeySecretEncrypted = pgKeySecretEncrypted;
    }

    public String getPgWebhookSecretEncrypted() {
        return pgWebhookSecretEncrypted;
    }

    public void setPgWebhookSecretEncrypted(String pgWebhookSecretEncrypted) {
        this.pgWebhookSecretEncrypted = pgWebhookSecretEncrypted;
    }
}
