package com.legalhelp.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record PgConfigUpdateRequest(@NotBlank String keyId, @NotBlank String keySecret, @NotBlank String webhookSecret) {
}
