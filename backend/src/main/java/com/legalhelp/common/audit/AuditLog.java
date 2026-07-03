package com.legalhelp.common.audit;

import jakarta.persistence.*;

import java.time.Instant;

/** Append-only. No update/delete methods exist anywhere in this codebase for this entity. */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_role", length = 32)
    private String actorRole;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(nullable = false, length = 128)
    private String entity;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Lob
    @Column(name = "before_state")
    private String beforeState;

    @Lob
    @Column(name = "after_state")
    private String afterState;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(Long actorId, String actorRole, String action, String entity, String entityId,
                     String beforeState, String afterState) {
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.beforeState = beforeState;
        this.afterState = afterState;
    }

    public Long getId() {
        return id;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getEntity() {
        return entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
