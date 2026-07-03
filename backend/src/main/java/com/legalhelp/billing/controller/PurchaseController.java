package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.PurchaseInitiateResponse;
import com.legalhelp.billing.service.PaymentService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
@PreAuthorize("hasRole('CUSTOMER')")
public class PurchaseController {

    private final PaymentService paymentService;

    public PurchaseController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/plans/{planId}")
    public PurchaseInitiateResponse initiate(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long planId) {
        return paymentService.initiatePurchase(principal.userId(), planId);
    }
}
