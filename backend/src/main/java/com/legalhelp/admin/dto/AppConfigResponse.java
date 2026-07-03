package com.legalhelp.admin.dto;

public record AppConfigResponse(int freeMinutes, long payoutThresholdMinorUnits, boolean pgConfigured) {
}
