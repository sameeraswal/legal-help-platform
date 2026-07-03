package com.legalhelp.billing.dto;

import com.legalhelp.billing.entity.LedgerEntryType;

import java.time.Instant;

public record LedgerEntryResponse(Long id, LedgerEntryType entryType, Long secondsDelta, Long amountDeltaMinorUnits,
                                   String reference, Instant createdAt) {
}
