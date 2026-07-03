package com.legalhelp.auth.dto;

import com.legalhelp.common.security.Role;

public record UserProfileResponse(Long id, Role role, String name, String email, String phone) {
}
