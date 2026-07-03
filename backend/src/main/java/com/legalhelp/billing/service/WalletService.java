package com.legalhelp.billing.service;

import com.legalhelp.admin.service.AppConfigService;
import com.legalhelp.billing.entity.CustomerWallet;
import com.legalhelp.billing.entity.LedgerEntryType;
import com.legalhelp.billing.entity.WalletLedger;
import com.legalhelp.billing.repository.CustomerWalletRepository;
import com.legalhelp.billing.repository.WalletLedgerRepository;
import com.legalhelp.common.exception.ApiException;
import com.legalhelp.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every mutation here takes a PESSIMISTIC_WRITE lock on the customer's wallet row and
 * writes exactly one new {@link WalletLedger} row in the same transaction — this is
 * what makes concurrent consume-vs-recharge safe (CLAUDE.md domain rule #2). Free
 * seconds are always consumed before paid seconds.
 */
@Service
public class WalletService {

    private final CustomerWalletRepository walletRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final AppConfigService appConfigService;

    public WalletService(CustomerWalletRepository walletRepository, WalletLedgerRepository ledgerRepository,
                          AppConfigService appConfigService) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.appConfigService = appConfigService;
    }

    @Transactional
    public CustomerWallet getOrCreateWallet(Long customerId) {
        return walletRepository.findByIdForUpdate(customerId)
                .orElseGet(() -> walletRepository.save(new CustomerWallet(customerId, appConfigService.getFreeSeconds())));
    }

    @Transactional(readOnly = true)
    public long availableSeconds(Long customerId) {
        return walletRepository.findByIdForUpdate(customerId)
                .map(CustomerWallet::totalAvailableSeconds)
                .orElseGet(appConfigService::getFreeSeconds);
    }

    /** Consumes free seconds first, then paid seconds. Throws if the wallet has insufficient balance. */
    @Transactional
    public void consumeSeconds(Long customerId, long seconds, String reference) {
        if (seconds <= 0) {
            return;
        }
        CustomerWallet wallet = getOrCreateWallet(customerId);
        if (wallet.totalAvailableSeconds() < seconds) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE, HttpStatus.PAYMENT_REQUIRED,
                    "Insufficient wallet balance to consume " + seconds + " seconds");
        }

        long fromFree = Math.min(wallet.getFreeSecondsRemaining(), seconds);
        long fromPaid = seconds - fromFree;
        wallet.setFreeSecondsRemaining(wallet.getFreeSecondsRemaining() - fromFree);
        wallet.setRemainingSeconds(wallet.getRemainingSeconds() - fromPaid);

        ledgerRepository.save(WalletLedger.customerEntry(customerId, LedgerEntryType.CONSUME, -seconds,
                wallet.totalAvailableSeconds(), reference));
    }

    @Transactional
    public void rechargeSeconds(Long customerId, long seconds, String reference) {
        CustomerWallet wallet = getOrCreateWallet(customerId);
        wallet.setRemainingSeconds(wallet.getRemainingSeconds() + seconds);
        ledgerRepository.save(WalletLedger.customerEntry(customerId, LedgerEntryType.RECHARGE, seconds,
                wallet.totalAvailableSeconds(), reference));
    }

    /**
     * Posts a compensating REFUND ledger entry — never mutates prior rows. {@code secondsDelta}
     * is signed: positive for a goodwill time credit, negative to claw back the seconds granted
     * by a payment that was refunded via the PG (capped so the paid balance never goes negative,
     * even if the customer already consumed some of those seconds).
     */
    @Transactional
    public void adjustSecondsWithRefundEntry(Long customerId, long secondsDelta, String reference) {
        CustomerWallet wallet = getOrCreateWallet(customerId);
        long newRemaining = Math.max(0, wallet.getRemainingSeconds() + secondsDelta);
        long actualDelta = newRemaining - wallet.getRemainingSeconds();
        wallet.setRemainingSeconds(newRemaining);
        ledgerRepository.save(WalletLedger.customerEntry(customerId, LedgerEntryType.REFUND, actualDelta,
                wallet.totalAvailableSeconds(), reference));
    }

    @Transactional(readOnly = true)
    public Page<WalletLedger> ledgerHistory(Long customerId, Pageable pageable) {
        return ledgerRepository.findByUserIdOrderByCreatedAtDesc(customerId, pageable);
    }
}
