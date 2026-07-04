package com.legalhelp.petition.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Regeneration always inserts a new row with an incremented version — an existing
 * Petition row is never updated (CLAUDE.md domain rule #10).
 */
@Entity
@Table(name = "petitions")
public class Petition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "generated_content", nullable = false, columnDefinition = "LONGTEXT")
    private String generatedContent;

    @Column(name = "pdf_url", nullable = false, length = 512)
    private String pdfUrl;

    @Column(name = "docx_url", nullable = false, length = 512)
    private String docxUrl;

    @Column(nullable = false)
    private int version;

    @Column(name = "disclaimer_version", nullable = false, length = 16)
    private String disclaimerVersion;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();

    protected Petition() {
    }

    public Petition(Long caseId, String generatedContent, String pdfUrl, String docxUrl, int version, String disclaimerVersion) {
        this.caseId = caseId;
        this.generatedContent = generatedContent;
        this.pdfUrl = pdfUrl;
        this.docxUrl = docxUrl;
        this.version = version;
        this.disclaimerVersion = disclaimerVersion;
    }

    public Long getId() {
        return id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getDocxUrl() {
        return docxUrl;
    }

    public int getVersion() {
        return version;
    }

    public String getDisclaimerVersion() {
        return disclaimerVersion;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
