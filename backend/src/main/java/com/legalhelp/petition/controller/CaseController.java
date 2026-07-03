package com.legalhelp.petition.controller;

import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.petition.dto.CaseDraftUpdateRequest;
import com.legalhelp.petition.dto.CaseIntakeRequest;
import com.legalhelp.petition.dto.CaseResponse;
import com.legalhelp.petition.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
@PreAuthorize("hasRole('CUSTOMER')")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public List<CaseResponse> listMine(@AuthenticationPrincipal AuthPrincipal principal) {
        return caseService.listForCustomer(principal.userId());
    }

    @GetMapping("/{caseId}")
    public CaseResponse get(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long caseId) {
        return caseService.getOwned(principal.userId(), caseId);
    }

    @PostMapping
    public CaseResponse startDraft(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CaseIntakeRequest request) {
        return caseService.startDraft(principal.userId(), request);
    }

    @PatchMapping("/{caseId}")
    public CaseResponse updateDraft(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long caseId,
                                     @RequestBody CaseDraftUpdateRequest request) {
        return caseService.updateDraft(principal.userId(), caseId, request);
    }

    @PostMapping("/{caseId}/submit")
    public CaseResponse submit(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long caseId) {
        return caseService.submit(principal.userId(), caseId);
    }
}
