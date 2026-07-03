package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.LawyerRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LawyerRateRepository extends JpaRepository<LawyerRate, Long> {
    Optional<LawyerRate> findFirstByLawyerIdOrderByEffectiveFromDesc(Long lawyerId);

    Optional<LawyerRate> findFirstByLawyerIdIsNullOrderByEffectiveFromDesc();

    List<LawyerRate> findByLawyerIdOrderByEffectiveFromDesc(Long lawyerId);
}
