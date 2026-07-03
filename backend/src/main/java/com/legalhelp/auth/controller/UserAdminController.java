package com.legalhelp.auth.controller;

import com.legalhelp.auth.dto.AdminUserResponse;
import com.legalhelp.auth.entity.UserStatus;
import com.legalhelp.auth.service.UserAdminService;
import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.common.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public Page<AdminUserResponse> list(@RequestParam Role role,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return userAdminService.listByRole(role, PageRequest.of(page, size));
    }

    @PostMapping("/{userId}/approve-lawyer")
    public AdminUserResponse approveLawyer(@PathVariable Long userId, @AuthenticationPrincipal AuthPrincipal actor) {
        return userAdminService.approveLawyer(userId, actor);
    }

    @PostMapping("/{userId}/status")
    public AdminUserResponse setStatus(@PathVariable Long userId, @RequestParam UserStatus status,
                                        @AuthenticationPrincipal AuthPrincipal actor) {
        return userAdminService.setStatus(userId, status, actor);
    }
}
