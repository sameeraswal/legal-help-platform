package com.legalhelp.billing.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** {@code lawyerId == null} is the global default rate. */
@Entity
@Table(name = "lawyer_rates")
public class LawyerRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lawyer_id")
    private Long lawyerId;

    @Column(name = "per_minute_rate_minor_units", nullable = false)
    private long perMinuteRateMinorUnits;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    protected LawyerRate() {
    }

    public LawyerRate(Long lawyerId, long perMinuteRateMinorUnits) {
        this.lawyerId = lawyerId;
        this.perMinuteRateMinorUnits = perMinuteRateMinorUnits;
    }

    public Long getId() {
        return id;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public long getPerMinuteRateMinorUnits() {
        return perMinuteRateMinorUnits;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }
}
