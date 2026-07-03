package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.RefundResponse;
import com.legalhelp.billing.service.RefundService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
public class RefundAdminController {

    private final RefundService refundService;

    public RefundAdminController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/{paymentId}/refund")
    public RefundResponse refund(@PathVariable Long paymentId, @AuthenticationPrincipal AuthPrincipal actor) {
        return refundService.initiateRefund(paymentId, actor);
    }
}
