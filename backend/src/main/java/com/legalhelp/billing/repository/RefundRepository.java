package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
