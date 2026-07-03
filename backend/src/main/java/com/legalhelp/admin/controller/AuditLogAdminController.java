package com.legalhelp.admin.controller;

import com.legalhelp.admin.dto.AuditLogResponse;
import com.legalhelp.common.audit.AuditLog;
import com.legalhelp.common.audit.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogAdminController {

    private final AuditLogService auditLogService;

    public AuditLogAdminController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Page<AuditLogResponse> list(@RequestParam(required = false) String entity,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditLog> result = entity != null
                ? auditLogService.findByEntity(entity, pageRequest)
                : auditLogService.findAll(pageRequest);
        return result.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorId(), log.getActorRole(), log.getAction(), log.getEntity(),
                log.getEntityId(), log.getBeforeState(), log.getAfterState(), log.getCreatedAt());
    }
}
