package com.legalhelp.petition.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "case_categories")
public class CaseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 1024)
    private String description;

    /** Name of the prompt template file under classpath:petition-templates/, without extension. */
    @Column(name = "template_key", nullable = false, length = 64)
    private String templateKey;

    @Column(nullable = false)
    private boolean active = true;

    protected CaseCategory() {
    }

    public CaseCategory(String slug, String name, String description, String templateKey) {
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.templateKey = templateKey;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
