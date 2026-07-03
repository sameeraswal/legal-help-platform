package com.legalhelp.petition.repository;

import com.legalhelp.petition.entity.Petition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition, Long> {
    List<Petition> findByCaseIdOrderByVersionDesc(Long caseId);

    Optional<Petition> findFirstByCaseIdOrderByVersionDesc(Long caseId);
}
