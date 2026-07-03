package com.legalhelp.petition.dto;

public record CaseCategoryResponse(Long id, String slug, String name, String description, boolean active) {
}
