package com.legalhelp.chat.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * `billedSeconds` is written only by the server-side {@code TimeMeteringService}
 * scheduled tick — never trust a client-reported duration (CLAUDE.md domain rule #1).
 */
@Entity
@Table(name = "chat_sessions")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterpart_type", nullable = false, length = 16)
    private CounterpartType counterpartType;

    /** Set only when counterpartType == LAWYER. */
    @Column(name = "lawyer_id")
    private Long lawyerId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_metered_at", nullable = false)
    private Instant lastMeteredAt = Instant.now();

    @Column(name = "billed_seconds", nullable = false)
    private long billedSeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ChatSessionStatus status = ChatSessionStatus.ACTIVE;

    protected ChatSession() {
    }

    public ChatSession(Long customerId, CounterpartType counterpartType, Long lawyerId) {
        this.customerId = customerId;
        this.counterpartType = counterpartType;
        this.lawyerId = lawyerId;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public CounterpartType getCounterpartType() {
        return counterpartType;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getLastMeteredAt() {
        return lastMeteredAt;
    }

    public void setLastMeteredAt(Instant lastMeteredAt) {
        this.lastMeteredAt = lastMeteredAt;
    }

    public long getBilledSeconds() {
        return billedSeconds;
    }

    public void addBilledSeconds(long seconds) {
        this.billedSeconds += seconds;
    }

    public ChatSessionStatus getStatus() {
        return status;
    }

    public void end() {
        this.status = ChatSessionStatus.ENDED;
        this.endedAt = Instant.now();
    }
}
