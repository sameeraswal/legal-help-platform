package com.legalhelp.billing.service;

import com.legalhelp.billing.entity.LawyerRate;
import com.legalhelp.billing.entity.LawyerWallet;
import com.legalhelp.billing.entity.LedgerEntryType;
import com.legalhelp.billing.entity.WalletLedger;
import com.legalhelp.billing.repository.LawyerRateRepository;
import com.legalhelp.billing.repository.LawyerWalletRepository;
import com.legalhelp.billing.repository.WalletLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Same row-locking discipline as {@link WalletService}, applied to lawyer earnings. */
@Service
public class LawyerWalletService {

    private final LawyerWalletRepository walletRepository;
    private final LawyerRateRepository rateRepository;
    private final WalletLedgerRepository ledgerRepository;

    public LawyerWalletService(LawyerWalletRepository walletRepository, LawyerRateRepository rateRepository,
                                WalletLedgerRepository ledgerRepository) {
        this.walletRepository = walletRepository;
        this.rateRepository = rateRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public LawyerWallet getOrCreateWallet(Long lawyerId) {
        return walletRepository.findByIdForUpdate(lawyerId)
                .orElseGet(() -> walletRepository.save(new LawyerWallet(lawyerId)));
    }

    @Transactional(readOnly = true)
    public long currentPerMinuteRateMinorUnits(Long lawyerId) {
        return rateRepository.findFirstByLawyerIdOrderByEffectiveFromDesc(lawyerId)
                .map(LawyerRate::getPerMinuteRateMinorUnits)
                .or(() -> rateRepository.findFirstByLawyerIdIsNullOrderByEffectiveFromDesc().map(LawyerRate::getPerMinuteRateMinorUnits))
                .orElse(0L);
    }

    /** Credits earnings for {@code seconds} of billed chat time at the lawyer's current per-minute rate. */
    @Transactional
    public void creditEarning(Long lawyerId, long seconds, String reference) {
        if (seconds <= 0) {
            return;
        }
        long perMinuteRate = currentPerMinuteRateMinorUnits(lawyerId);
        long amount = BigDecimal.valueOf(perMinuteRate)
                .multiply(BigDecimal.valueOf(seconds))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP)
                .longValueExact();
        if (amount <= 0) {
            return;
        }
        LawyerWallet wallet = getOrCreateWallet(lawyerId);
        wallet.setBalanceMinorUnits(wallet.getBalanceMinorUnits() + amount);
        ledgerRepository.save(WalletLedger.lawyerEntry(lawyerId, LedgerEntryType.EARNING, amount,
                wallet.getBalanceMinorUnits(), reference));
    }

    /** Debits the wallet when a payout is approved and paid. Throws if the balance is insufficient. */
    @Transactional
    public void debitForPayout(Long lawyerId, long amount, String reference) {
        LawyerWallet wallet = getOrCreateWallet(lawyerId);
        if (wallet.getBalanceMinorUnits() < amount) {
            throw new IllegalStateException("Lawyer wallet balance is insufficient for this payout");
        }
        wallet.setBalanceMinorUnits(wallet.getBalanceMinorUnits() - amount);
        ledgerRepository.save(WalletLedger.lawyerEntry(lawyerId, LedgerEntryType.PAYOUT, -amount,
                wallet.getBalanceMinorUnits(), reference));
    }
}
