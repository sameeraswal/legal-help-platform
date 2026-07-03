package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.PlanResponse;
import com.legalhelp.billing.dto.PlanUpsertRequest;
import com.legalhelp.billing.service.PlanService;
import com.legalhelp.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/plans")
public class PlanAdminController {

    private final PlanService planService;

    public PlanAdminController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanResponse> listAll() {
        return planService.listAll();
    }

    @PostMapping
    public PlanResponse create(@Valid @RequestBody PlanUpsertRequest request, @AuthenticationPrincipal AuthPrincipal actor) {
        return planService.create(request, actor);
    }

    @PutMapping("/{id}")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody PlanUpsertRequest request,
                                @AuthenticationPrincipal AuthPrincipal actor) {
        return planService.update(id, request, actor);
    }
}
