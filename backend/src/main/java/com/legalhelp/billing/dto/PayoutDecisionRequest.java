package com.legalhelp.billing.dto;

public record PayoutDecisionRequest(boolean approve, String bankReference) {
}
