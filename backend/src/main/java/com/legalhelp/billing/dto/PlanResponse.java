package com.legalhelp.billing.dto;

public record PlanResponse(Long id, String name, long priceMinorUnits, int seconds, boolean active) {
}
