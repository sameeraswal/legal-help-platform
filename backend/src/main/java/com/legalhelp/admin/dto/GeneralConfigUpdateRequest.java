package com.legalhelp.admin.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record GeneralConfigUpdateRequest(@PositiveOrZero int freeMinutes, @PositiveOrZero long payoutThresholdMinorUnits) {
}
