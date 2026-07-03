package com.legalhelp.billing.service;

import com.legalhelp.billing.dto.PlanResponse;
import com.legalhelp.billing.dto.PlanUpsertRequest;
import com.legalhelp.billing.entity.Plan;
import com.legalhelp.billing.repository.PlanRepository;
import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository repository;
    private final AuditLogService auditLogService;

    public PlanService(PlanRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listActive() {
        return repository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Plan getRequired(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
    }

    @Transactional
    public PlanResponse create(PlanUpsertRequest request, AuthPrincipal actor) {
        Plan plan = new Plan(request.name(), request.priceMinorUnits(), request.seconds());
        plan.setActive(request.active());
        plan = repository.save(plan);
        auditLogService.record(actor.userId(), actor.role(), "CREATE", "plan", String.valueOf(plan.getId()), null, toResponse(plan));
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse update(Long id, PlanUpsertRequest request, AuthPrincipal actor) {
        Plan plan = getRequired(id);
        PlanResponse before = toResponse(plan);
        plan.setName(request.name());
        plan.setPriceMinorUnits(request.priceMinorUnits());
        plan.setSeconds(request.seconds());
        plan.setActive(request.active());
        auditLogService.record(actor.userId(), actor.role(), "UPDATE", "plan", String.valueOf(id), before, toResponse(plan));
        return toResponse(plan);
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getName(), plan.getPriceMinorUnits(), plan.getSeconds(), plan.isActive());
    }
}
