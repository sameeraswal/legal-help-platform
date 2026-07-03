package com.legalhelp.petition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CaseCategoryUpsertRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,64}$", message = "must be lowercase, alphanumeric, and hyphens only") String slug,
        @NotBlank String name,
        String description,
        @NotBlank String templateKey,
        boolean active
) {
}
