package com.legalhelp.auth.dto;

import com.legalhelp.common.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull Role role,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Email String email,
        String phone,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
