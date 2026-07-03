package com.legalhelp.billing.dto;

public record WalletBalanceResponse(long freeSecondsRemaining, long paidSecondsRemaining, long totalSecondsAvailable) {
}
