package com.legalhelp.admin.dto;

import java.time.Instant;

public record AuditLogResponse(Long id, Long actorId, String actorRole, String action, String entity, String entityId,
                                String beforeState, String afterState, Instant createdAt) {
}
