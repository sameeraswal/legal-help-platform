package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.PayoutRequest;
import com.legalhelp.billing.entity.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
    Page<PayoutRequest> findByStatus(PayoutStatus status, Pageable pageable);

    Page<PayoutRequest> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId, Pageable pageable);
}
