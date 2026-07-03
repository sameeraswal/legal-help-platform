package com.legalhelp.auth.service;

import com.legalhelp.auth.dto.AdminUserResponse;
import com.legalhelp.auth.entity.User;
import com.legalhelp.auth.entity.UserStatus;
import com.legalhelp.auth.repository.UserRepository;
import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.common.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserAdminService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse approveLawyer(Long lawyerId, AuthPrincipal actor) {
        User user = userRepository.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.LAWYER) {
            throw new BadRequestException("Only lawyer accounts can be approved");
        }
        user.setLawyerApproved(true);
        auditLogService.record(actor.userId(), actor.role(), "APPROVE_LAWYER", "user", String.valueOf(lawyerId), null, toResponse(user));
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse setStatus(Long userId, UserStatus status, AuthPrincipal actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserStatus before = user.getStatus();
        user.setStatus(status);
        auditLogService.record(actor.userId(), actor.role(), "SET_USER_STATUS", "user", String.valueOf(userId), before, status);
        return toResponse(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getRole(), user.getName(), user.getEmail(), user.getPhone(),
                user.getStatus(), user.getLawyerApproved(), user.getCreatedAt());
    }
}
