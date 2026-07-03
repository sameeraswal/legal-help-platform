package com.legalhelp.billing.dto;

import java.time.Instant;

public record LawyerRateResponse(Long id, Long lawyerId, long perMinuteRateMinorUnits, Instant effectiveFrom) {
}
