package com.legalhelp.chat.dto;

import jakarta.validation.constraints.NotNull;

public record StartLawyerSessionRequest(@NotNull Long lawyerId) {
}
