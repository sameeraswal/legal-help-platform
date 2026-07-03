package com.legalhelp.chat.dto;

import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.entity.CounterpartType;

import java.time.Instant;

public record ChatSessionResponse(Long id, Long customerId, CounterpartType counterpartType, Long lawyerId,
                                   ChatSessionStatus status, long billedSeconds, Instant startedAt, Instant endedAt) {
}
