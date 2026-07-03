package com.legalhelp.billing.dto;

import com.legalhelp.billing.entity.RefundStatus;

import java.time.Instant;

public record RefundResponse(Long id, Long paymentId, long amountMinorUnits, RefundStatus status, Instant createdAt) {
}
