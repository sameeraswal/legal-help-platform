package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.PaymentResponse;
import com.legalhelp.billing.entity.PaymentStatus;
import com.legalhelp.billing.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/transactions")
public class TransactionAdminController {

    private final PaymentService paymentService;

    public TransactionAdminController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public Page<PaymentResponse> list(@RequestParam(required = false) PaymentStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return status != null ? paymentService.listByStatus(status, pageRequest) : paymentService.listAll(pageRequest);
    }
}
