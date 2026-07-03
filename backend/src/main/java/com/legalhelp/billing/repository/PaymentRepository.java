package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.Payment;
import com.legalhelp.billing.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);

    boolean existsByPgRef(String pgRef);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /** Locked for the duration of webhook processing so a replayed/duplicate delivery can't double-credit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.pgOrderId = :pgOrderId")
    Optional<Payment> findByPgOrderIdForUpdate(String pgOrderId);
}
