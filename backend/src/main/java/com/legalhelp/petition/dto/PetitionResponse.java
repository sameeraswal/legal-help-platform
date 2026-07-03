package com.legalhelp.petition.dto;

import java.time.Instant;

public record PetitionResponse(Long id, Long caseId, String pdfUrl, String docxUrl, int version,
                                String disclaimerVersion, Instant generatedAt) {
}
