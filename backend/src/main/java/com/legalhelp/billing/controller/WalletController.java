package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.LedgerEntryResponse;
import com.legalhelp.billing.dto.WalletBalanceResponse;
import com.legalhelp.billing.entity.CustomerWallet;
import com.legalhelp.billing.entity.WalletLedger;
import com.legalhelp.billing.service.WalletService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@PreAuthorize("hasRole('CUSTOMER')")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public WalletBalanceResponse balance(@AuthenticationPrincipal AuthPrincipal principal) {
        CustomerWallet wallet = walletService.getOrCreateWallet(principal.userId());
        return new WalletBalanceResponse(wallet.getFreeSecondsRemaining(), wallet.getRemainingSeconds(),
                wallet.totalAvailableSeconds());
    }

    @GetMapping("/transactions")
    public Page<LedgerEntryResponse> transactions(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return walletService.ledgerHistory(principal.userId(), PageRequest.of(page, size)).map(this::toResponse);
    }

    private LedgerEntryResponse toResponse(WalletLedger entry) {
        return new LedgerEntryResponse(entry.getId(), entry.getEntryType(), entry.getSecondsDelta(),
                entry.getAmountDeltaMinorUnits(), entry.getReference(), entry.getCreatedAt());
    }
}
