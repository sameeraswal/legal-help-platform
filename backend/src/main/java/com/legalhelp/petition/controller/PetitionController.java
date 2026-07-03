package com.legalhelp.petition.controller;

import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.petition.dto.PetitionResponse;
import com.legalhelp.petition.service.PetitionGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases/{caseId}/petitions")
@PreAuthorize("hasRole('CUSTOMER')")
public class PetitionController {

    private final PetitionGenerationService petitionGenerationService;

    public PetitionController(PetitionGenerationService petitionGenerationService) {
        this.petitionGenerationService = petitionGenerationService;
    }

    @PostMapping("/generate")
    public PetitionResponse generate(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long caseId) {
        return petitionGenerationService.generate(principal.userId(), caseId);
    }

    @GetMapping
    public List<PetitionResponse> listVersions(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long caseId) {
        return petitionGenerationService.listVersions(principal.userId(), caseId);
    }

    @GetMapping("/{petitionId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@AuthenticationPrincipal AuthPrincipal principal,
                                               @PathVariable Long caseId, @PathVariable Long petitionId) {
        byte[] content = petitionGenerationService.downloadPdf(principal.userId(), caseId, petitionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"petition-" + petitionId + ".pdf\"")
                .body(content);
    }

    @GetMapping("/{petitionId}/docx")
    public ResponseEntity<byte[]> downloadDocx(@AuthenticationPrincipal AuthPrincipal principal,
                                                @PathVariable Long caseId, @PathVariable Long petitionId) {
        byte[] content = petitionGenerationService.downloadDocx(principal.userId(), caseId, petitionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"petition-" + petitionId + ".docx\"")
                .body(content);
    }
}
