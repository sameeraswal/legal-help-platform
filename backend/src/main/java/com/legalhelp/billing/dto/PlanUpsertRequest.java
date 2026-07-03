package com.legalhelp.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PlanUpsertRequest(
        @NotBlank String name,
        @Positive long priceMinorUnits,
        @Positive int seconds,
        boolean active
) {
}
