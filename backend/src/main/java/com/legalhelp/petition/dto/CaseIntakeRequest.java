package com.legalhelp.petition.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CaseIntakeRequest(@NotNull Long categoryId, Map<String, Object> details) {
}
