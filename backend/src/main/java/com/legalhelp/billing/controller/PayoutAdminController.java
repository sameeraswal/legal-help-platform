package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.PayoutDecisionRequest;
import com.legalhelp.billing.dto.PayoutRequestResponse;
import com.legalhelp.billing.service.PayoutService;
import com.legalhelp.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payout-requests")
public class PayoutAdminController {

    private final PayoutService payoutService;

    public PayoutAdminController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @GetMapping
    public Page<PayoutRequestResponse> listPending(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return payoutService.listPending(PageRequest.of(page, size));
    }

    @PostMapping("/{id}/decision")
    public PayoutRequestResponse decide(@PathVariable Long id, @Valid @RequestBody PayoutDecisionRequest decision,
                                         @AuthenticationPrincipal AuthPrincipal actor) {
        return payoutService.decide(id, decision, actor);
    }
}
