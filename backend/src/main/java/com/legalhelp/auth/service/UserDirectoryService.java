package com.legalhelp.auth.service;

import com.legalhelp.auth.dto.UserProfileResponse;
import com.legalhelp.auth.entity.User;
import com.legalhelp.auth.repository.UserRepository;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * The only auth-module surface other modules should call for basic user lookups —
 * they must never reach into UserRepository/User directly (CLAUDE.md module
 * boundary rule).
 */
@Service
public class UserDirectoryService {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return toResponse(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Transactional(readOnly = true)
    public Map<Long, UserProfileResponse> getProfiles(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> listApprovedLawyers() {
        return userRepository.findByRole(Role.LAWYER, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(u -> Boolean.TRUE.equals(u.getLawyerApproved()))
                .map(this::toResponse)
                .toList();
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getRole(), user.getName(), user.getEmail(), user.getPhone());
    }
}
