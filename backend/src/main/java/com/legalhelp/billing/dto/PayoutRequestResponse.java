package com.legalhelp.billing.dto;

import com.legalhelp.billing.entity.PayoutStatus;

import java.time.Instant;

public record PayoutRequestResponse(Long id, Long lawyerId, long amountMinorUnits, PayoutStatus status,
                                     String bankReference, Instant createdAt, Instant decidedAt) {
}
