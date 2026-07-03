package com.legalhelp.petition.dto;

import com.legalhelp.petition.entity.CaseStatus;

import java.time.Instant;
import java.util.Map;

public record CaseResponse(Long id, Long categoryId, Map<String, Object> details, CaseStatus status,
                            Instant createdAt, Instant updatedAt) {
}
