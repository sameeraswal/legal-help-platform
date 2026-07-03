package com.legalhelp.admin.service;

import com.legalhelp.admin.entity.AppConfig;
import com.legalhelp.admin.repository.AppConfigRepository;
import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.crypto.EncryptionService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Single source of truth for platform-wide, admin-editable settings. Other modules
 * (billing, chat) depend on this service — never on admin's entities/repositories
 * directly — to read free-minutes/payout-threshold/PG-credential values.
 */
@Service
public class AppConfigService {

    private static final Long SINGLETON_ID = 1L;

    private final AppConfigRepository repository;
    private final EncryptionService encryptionService;
    private final AuditLogService auditLogService;
    private final int defaultFreeMinutes;
    private final long defaultPayoutThresholdMinorUnits;
    private final String envPgKeyId;
    private final String envPgKeySecret;
    private final String envPgWebhookSecret;

    public AppConfigService(AppConfigRepository repository,
                             EncryptionService encryptionService,
                             AuditLogService auditLogService,
                             @Value("${app.chat.default-free-minutes:30}") int defaultFreeMinutes,
                             @Value("${app.billing.default-payout-threshold-minor-units:100000}") long defaultPayoutThresholdMinorUnits,
                             @Value("${app.payment.razorpay.key-id}") String envPgKeyId,
                             @Value("${app.payment.razorpay.key-secret}") String envPgKeySecret,
                             @Value("${app.payment.razorpay.webhook-secret}") String envPgWebhookSecret) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.auditLogService = auditLogService;
        this.defaultFreeMinutes = defaultFreeMinutes;
        this.defaultPayoutThresholdMinorUnits = defaultPayoutThresholdMinorUnits;
        this.envPgKeyId = envPgKeyId;
        this.envPgKeySecret = envPgKeySecret;
        this.envPgWebhookSecret = envPgWebhookSecret;
    }

    @Transactional
    public AppConfig getOrCreate() {
        return repository.findById(SINGLETON_ID)
                .orElseGet(() -> repository.save(new AppConfig(defaultFreeMinutes, defaultPayoutThresholdMinorUnits)));
    }

    @Transactional(readOnly = true)
    public long getFreeSeconds() {
        return getOrCreate().getFreeMinutes() * 60L;
    }

    @Transactional(readOnly = true)
    public long getPayoutThresholdMinorUnits() {
        return getOrCreate().getPayoutThresholdMinorUnits();
    }

    @Transactional(readOnly = true)
    public String getPgKeyId() {
        return decryptOrDefault(getOrCreate().getPgKeyIdEncrypted(), envPgKeyId);
    }

    @Transactional(readOnly = true)
    public String getPgKeySecret() {
        return decryptOrDefault(getOrCreate().getPgKeySecretEncrypted(), envPgKeySecret);
    }

    @Transactional(readOnly = true)
    public String getPgWebhookSecret() {
        return decryptOrDefault(getOrCreate().getPgWebhookSecretEncrypted(), envPgWebhookSecret);
    }

    @Transactional(readOnly = true)
    public boolean isPgConfiguredExplicitly() {
        return getOrCreate().getPgKeyIdEncrypted() != null;
    }

    @Transactional
    public void updateGeneralConfig(int freeMinutes, long payoutThresholdMinorUnits, AuthPrincipal actor) {
        AppConfig config = getOrCreate();
        Object before = snapshot(config);
        config.setFreeMinutes(freeMinutes);
        config.setPayoutThresholdMinorUnits(payoutThresholdMinorUnits);
        auditLogService.record(actor.userId(), actor.role(), "UPDATE", "app_config", "1", before, snapshot(config));
    }

    @Transactional
    public void updatePgConfig(String keyId, String keySecret, String webhookSecret, AuthPrincipal actor) {
        AppConfig config = getOrCreate();
        config.setPgKeyIdEncrypted(encryptionService.encrypt(keyId));
        config.setPgKeySecretEncrypted(encryptionService.encrypt(keySecret));
        config.setPgWebhookSecretEncrypted(encryptionService.encrypt(webhookSecret));
        // Never log the plaintext or ciphertext of a secret rotation — only that it happened.
        auditLogService.record(actor.userId(), actor.role(), "UPDATE_PG_CREDENTIALS", "app_config", "1", null, null);
    }

    private String decryptOrDefault(String encrypted, String envDefault) {
        return Optional.ofNullable(encrypted).map(encryptionService::decrypt).orElse(envDefault);
    }

    private Object snapshot(AppConfig config) {
        return new GeneralConfigSnapshot(config.getFreeMinutes(), config.getPayoutThresholdMinorUnits());
    }

    private record GeneralConfigSnapshot(int freeMinutes, long payoutThresholdMinorUnits) {
    }
}
