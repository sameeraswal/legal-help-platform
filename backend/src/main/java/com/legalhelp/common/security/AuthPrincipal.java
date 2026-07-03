package com.legalhelp.common.security;

/** Authenticated principal extracted from a validated JWT. */
public record AuthPrincipal(Long userId, String email, String role) {
}
