package com.legalhelp.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lawyer_wallets")
public class LawyerWallet {

    @Id
    @Column(name = "lawyer_id")
    private Long lawyerId;

    /** Balance in minor units (paise). */
    @Column(name = "balance_minor_units", nullable = false)
    private long balanceMinorUnits;

    protected LawyerWallet() {
    }

    public LawyerWallet(Long lawyerId) {
        this.lawyerId = lawyerId;
        this.balanceMinorUnits = 0;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public long getBalanceMinorUnits() {
        return balanceMinorUnits;
    }

    public void setBalanceMinorUnits(long balanceMinorUnits) {
        this.balanceMinorUnits = balanceMinorUnits;
    }
}
