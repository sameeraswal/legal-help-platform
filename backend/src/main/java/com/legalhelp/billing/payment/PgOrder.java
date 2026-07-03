package com.legalhelp.billing.payment;

public record PgOrder(String pgOrderId, long amountMinorUnits, String currency) {
}
