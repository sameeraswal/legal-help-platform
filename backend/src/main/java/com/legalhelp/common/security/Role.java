package com.legalhelp.common.security;

/** Shared across modules (unlike entities/repositories, which stay module-private per CLAUDE.md). */
public enum Role {
    CUSTOMER,
    LAWYER,
    ADMIN
}
