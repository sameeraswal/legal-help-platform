package com.legalhelp.auth.dto;

import com.legalhelp.auth.entity.UserStatus;
import com.legalhelp.common.security.Role;

import java.time.Instant;

public record AdminUserResponse(Long id, Role role, String name, String email, String phone, UserStatus status,
                                 Boolean lawyerApproved, Instant createdAt) {
}
