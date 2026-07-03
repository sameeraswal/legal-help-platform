package com.legalhelp.petition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.llm.LlmClient;
import com.legalhelp.petition.dto.PetitionResponse;
import com.legalhelp.petition.entity.Case;
import com.legalhelp.petition.entity.CaseCategory;
import com.legalhelp.petition.entity.CaseStatus;
import com.legalhelp.petition.entity.Petition;
import com.legalhelp.petition.repository.PetitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Intake data -> category prompt template -> LLM structured draft -> PDF/DOCX export.
 * Regeneration always creates a new Petition row (a new version); nothing is ever
 * overwritten (CLAUDE.md domain rule #10).
 */
@Service
public class PetitionGenerationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CaseService caseService;
    private final CaseCategoryService caseCategoryService;
    private final PromptTemplateLoader templateLoader;
    private final LlmClient llmClient;
    private final PdfGenerator pdfGenerator;
    private final DocxGenerator docxGenerator;
    private final PetitionStorageService storageService;
    private final PetitionRepository petitionRepository;

    public PetitionGenerationService(CaseService caseService, CaseCategoryService caseCategoryService,
                                      PromptTemplateLoader templateLoader, LlmClient llmClient,
                                      PdfGenerator pdfGenerator, DocxGenerator docxGenerator,
                                      PetitionStorageService storageService, PetitionRepository petitionRepository) {
        this.caseService = caseService;
        this.caseCategoryService = caseCategoryService;
        this.templateLoader = templateLoader;
        this.llmClient = llmClient;
        this.pdfGenerator = pdfGenerator;
        this.docxGenerator = docxGenerator;
        this.storageService = storageService;
        this.petitionRepository = petitionRepository;
    }

    @Transactional
    public PetitionResponse generate(Long customerId, Long caseId) {
        Case caseEntity = caseService.getOwnedCase(customerId, caseId);
        if (caseEntity.getStatus() == CaseStatus.DRAFT) {
            throw new BadRequestException("Submit the case before generating a petition");
        }
        CaseCategory category = caseCategoryService.getRequired(caseEntity.getCategoryId());

        String systemPrompt = templateLoader.load(category.getTemplateKey());
        String userPrompt = buildUserPrompt(caseEntity);
        String draftContent = llmClient.generate(systemPrompt, List.of(), userPrompt);

        int nextVersion = petitionRepository.findFirstByCaseIdOrderByVersionDesc(caseId)
                .map(p -> p.getVersion() + 1)
                .orElse(1);

        String title = category.getName() + " — Petition (v" + nextVersion + ")";
        byte[] pdfBytes = pdfGenerator.generate(title, draftContent, PetitionDisclaimer.TEXT);
        byte[] docxBytes = docxGenerator.generate(title, draftContent, PetitionDisclaimer.TEXT);

        String pdfFileName = "case-%d/petition-v%d.pdf".formatted(caseId, nextVersion);
        String docxFileName = "case-%d/petition-v%d.docx".formatted(caseId, nextVersion);
        storageService.write(pdfFileName, pdfBytes);
        storageService.write(docxFileName, docxBytes);

        Petition petition = new Petition(caseId, draftContent, pdfFileName, docxFileName, nextVersion, PetitionDisclaimer.CURRENT_VERSION);
        petition = petitionRepository.save(petition);
        caseEntity.setStatus(CaseStatus.PETITION_GENERATED);

        return toResponse(petition);
    }

    @Transactional(readOnly = true)
    public List<PetitionResponse> listVersions(Long customerId, Long caseId) {
        caseService.getOwnedCase(customerId, caseId);
        return petitionRepository.findByCaseIdOrderByVersionDesc(caseId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(Long customerId, Long caseId, Long petitionId) {
        Petition petition = requireOwnedPetition(customerId, caseId, petitionId);
        return storageService.read(petition.getPdfUrl());
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocx(Long customerId, Long caseId, Long petitionId) {
        Petition petition = requireOwnedPetition(customerId, caseId, petitionId);
        return storageService.read(petition.getDocxUrl());
    }

    private Petition requireOwnedPetition(Long customerId, Long caseId, Long petitionId) {
        caseService.getOwnedCase(customerId, caseId);
        return petitionRepository.findById(petitionId)
                .filter(p -> p.getCaseId().equals(caseId))
                .orElseThrow(() -> new BadRequestException("Petition not found for this case"));
    }

    private String buildUserPrompt(Case caseEntity) {
        try {
            String detailsJson = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(caseEntity.getDetails());
            return """
                    Draft a filing-ready petition based on the following structured case intake data. \
                    Use clear, formal legal drafting language appropriate for an Indian court filing. \
                    Do not include the disclaimer yourself — it is appended separately.

                    Case intake data (JSON):
                    %s""".formatted(detailsJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize case details for prompt", e);
        }
    }

    private PetitionResponse toResponse(Petition petition) {
        return new PetitionResponse(petition.getId(), petition.getCaseId(), petition.getPdfUrl(), petition.getDocxUrl(),
                petition.getVersion(), petition.getDisclaimerVersion(), petition.getGeneratedAt());
    }
}
