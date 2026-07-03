package com.legalhelp.billing.dto;

import com.legalhelp.billing.entity.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(Long id, String orderId, Long customerId, Long planId, long amountMinorUnits,
                               PaymentStatus status, Instant createdAt) {
}
