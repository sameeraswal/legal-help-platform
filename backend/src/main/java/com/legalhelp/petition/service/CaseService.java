package com.legalhelp.petition.service;

import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.petition.dto.CaseDraftUpdateRequest;
import com.legalhelp.petition.dto.CaseIntakeRequest;
import com.legalhelp.petition.dto.CaseResponse;
import com.legalhelp.petition.entity.Case;
import com.legalhelp.petition.entity.CaseStatus;
import com.legalhelp.petition.repository.CaseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseCategoryService caseCategoryService;

    public CaseService(CaseRepository caseRepository, CaseCategoryService caseCategoryService) {
        this.caseRepository = caseRepository;
        this.caseCategoryService = caseCategoryService;
    }

    @Transactional
    public CaseResponse startDraft(Long customerId, CaseIntakeRequest request) {
        caseCategoryService.getRequired(request.categoryId());
        Case caseEntity = new Case(customerId, request.categoryId());
        if (request.details() != null) {
            caseEntity.setDetails(new LinkedHashMap<>(request.details()));
        }
        caseEntity = caseRepository.save(caseEntity);
        return toResponse(caseEntity);
    }

    @Transactional
    public CaseResponse updateDraft(Long customerId, Long caseId, CaseDraftUpdateRequest request) {
        Case caseEntity = getOwnedCase(customerId, caseId);
        if (caseEntity.getStatus() != CaseStatus.DRAFT) {
            throw new BadRequestException("Only draft cases can be edited");
        }
        caseEntity.setDetails(request.details() != null ? new LinkedHashMap<>(request.details()) : caseEntity.getDetails());
        return toResponse(caseEntity);
    }

    @Transactional
    public CaseResponse submit(Long customerId, Long caseId) {
        Case caseEntity = getOwnedCase(customerId, caseId);
        if (caseEntity.getStatus() != CaseStatus.DRAFT) {
            throw new BadRequestException("Case has already been submitted");
        }
        caseEntity.setStatus(CaseStatus.SUBMITTED);
        return toResponse(caseEntity);
    }

    @Transactional(readOnly = true)
    public CaseResponse getOwned(Long customerId, Long caseId) {
        return toResponse(getOwnedCase(customerId, caseId));
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> listForCustomer(Long customerId) {
        return caseRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Case getOwnedCase(Long customerId, Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
        if (!caseEntity.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("This case does not belong to the current customer");
        }
        return caseEntity;
    }

    private CaseResponse toResponse(Case caseEntity) {
        return new CaseResponse(caseEntity.getId(), caseEntity.getCategoryId(), caseEntity.getDetails(),
                caseEntity.getStatus(), caseEntity.getCreatedAt(), caseEntity.getUpdatedAt());
    }
}
