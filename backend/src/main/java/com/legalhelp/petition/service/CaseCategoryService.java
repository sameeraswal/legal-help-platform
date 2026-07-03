package com.legalhelp.petition.service;

import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.petition.dto.CaseCategoryResponse;
import com.legalhelp.petition.dto.CaseCategoryUpsertRequest;
import com.legalhelp.petition.entity.CaseCategory;
import com.legalhelp.petition.repository.CaseCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CaseCategoryService {

    private final CaseCategoryRepository repository;
    private final AuditLogService auditLogService;

    public CaseCategoryService(CaseCategoryRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CaseCategoryResponse> listActive() {
        return repository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CaseCategoryResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CaseCategory getRequired(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Case category not found"));
    }

    @Transactional
    public CaseCategoryResponse create(CaseCategoryUpsertRequest request, AuthPrincipal actor) {
        if (repository.existsBySlug(request.slug())) {
            throw new BadRequestException("A category with this slug already exists");
        }
        CaseCategory category = new CaseCategory(request.slug(), request.name(), request.description(), request.templateKey());
        category.setActive(request.active());
        category = repository.save(category);
        auditLogService.record(actor.userId(), actor.role(), "CREATE", "case_category", String.valueOf(category.getId()), null, toResponse(category));
        return toResponse(category);
    }

    @Transactional
    public CaseCategoryResponse update(Long id, CaseCategoryUpsertRequest request, AuthPrincipal actor) {
        CaseCategory category = getRequired(id);
        CaseCategoryResponse before = toResponse(category);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setTemplateKey(request.templateKey());
        category.setActive(request.active());
        auditLogService.record(actor.userId(), actor.role(), "UPDATE", "case_category", String.valueOf(id), before, toResponse(category));
        return toResponse(category);
    }

    private CaseCategoryResponse toResponse(CaseCategory category) {
        return new CaseCategoryResponse(category.getId(), category.getSlug(), category.getName(),
                category.getDescription(), category.getTemplateKey(), category.isActive());
    }
}
