package com.legalhelp.billing.dto;

public record PurchaseInitiateResponse(String orderId, String pgOrderId, long amountMinorUnits, String currency, String pgKeyId) {
}
