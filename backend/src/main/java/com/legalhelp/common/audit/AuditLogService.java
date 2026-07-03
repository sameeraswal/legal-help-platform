package com.legalhelp.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Writes an audit entry in its own transaction so an audit failure never
     * rolls back, and a rollback of the caller's transaction never drops the audit
     * trail of what was attempted.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String actorRole, String action, String entity, String entityId,
                        Object before, Object after) {
        try {
            String beforeJson = before != null ? objectMapper.writeValueAsString(before) : null;
            String afterJson = after != null ? objectMapper.writeValueAsString(after) : null;
            repository.save(new AuditLog(actorId, actorRole, action, entity, entityId, beforeJson, afterJson));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit payload for action={} entity={} entityId={}", action, entity, entityId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntity(String entity, Pageable pageable) {
        return repository.findByEntityOrderByCreatedAtDesc(entity, pageable);
    }
}
