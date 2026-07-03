package com.legalhelp.billing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    /** Price in minor units (paise). */
    @Column(name = "price_minor_units", nullable = false)
    private long priceMinorUnits;

    @Column(name = "seconds", nullable = false)
    private int seconds;

    @Column(nullable = false)
    private boolean active = true;

    protected Plan() {
    }

    public Plan(String name, long priceMinorUnits, int seconds) {
        this.name = name;
        this.priceMinorUnits = priceMinorUnits;
        this.seconds = seconds;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getPriceMinorUnits() {
        return priceMinorUnits;
    }

    public void setPriceMinorUnits(long priceMinorUnits) {
        this.priceMinorUnits = priceMinorUnits;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
