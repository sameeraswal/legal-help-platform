package com.legalhelp.petition.repository;

import com.legalhelp.petition.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long> {
    List<Case> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);
}
