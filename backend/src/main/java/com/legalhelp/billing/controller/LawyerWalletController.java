package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.PayoutRequestResponse;
import com.legalhelp.billing.dto.WalletBalanceResponse;
import com.legalhelp.billing.entity.LawyerWallet;
import com.legalhelp.billing.service.LawyerWalletService;
import com.legalhelp.billing.service.PayoutService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lawyer/wallet")
@PreAuthorize("hasRole('LAWYER')")
public class LawyerWalletController {

    private final LawyerWalletService lawyerWalletService;
    private final PayoutService payoutService;

    public LawyerWalletController(LawyerWalletService lawyerWalletService, PayoutService payoutService) {
        this.lawyerWalletService = lawyerWalletService;
        this.payoutService = payoutService;
    }

    @GetMapping("/balance")
    public WalletBalanceResponse balance(@AuthenticationPrincipal AuthPrincipal principal) {
        LawyerWallet wallet = lawyerWalletService.getOrCreateWallet(principal.userId());
        return new WalletBalanceResponse(0, wallet.getBalanceMinorUnits(), wallet.getBalanceMinorUnits());
    }

    @PostMapping("/payout-requests")
    public PayoutRequestResponse requestPayout(@AuthenticationPrincipal AuthPrincipal principal) {
        return payoutService.requestPayout(principal.userId());
    }

    @GetMapping("/payout-requests")
    public Page<PayoutRequestResponse> myPayoutRequests(@AuthenticationPrincipal AuthPrincipal principal,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return payoutService.listForLawyer(principal.userId(), PageRequest.of(page, size));
    }
}
