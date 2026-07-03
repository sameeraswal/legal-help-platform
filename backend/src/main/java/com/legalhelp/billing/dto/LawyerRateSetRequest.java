package com.legalhelp.billing.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record LawyerRateSetRequest(Long lawyerId, @PositiveOrZero long perMinuteRateMinorUnits) {
}
