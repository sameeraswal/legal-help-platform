package com.legalhelp.petition.repository;

import com.legalhelp.petition.entity.CaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaseCategoryRepository extends JpaRepository<CaseCategory, Long> {
    List<CaseCategory> findByActiveTrue();

    Optional<CaseCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
