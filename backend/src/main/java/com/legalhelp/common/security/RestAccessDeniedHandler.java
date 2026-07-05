package com.legalhelp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalhelp.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles role-based rejections from {@code authorizeHttpRequests} (e.g. a CUSTOMER token
 * hitting {@code /api/admin/**}) — i.e. requests that authenticated successfully but lack the
 * required role. Spring Security's default handler here returns 403 with no body; this returns
 * the same {@code {code, message, traceId}} shape as {@code GlobalExceptionHandler}'s
 * {@code AccessDeniedException} handler, for consistency and so the frontend always has a
 * message to show. Distinct from {@link RestAuthenticationEntryPoint} (401, not authenticated at
 * all) — the frontend must not attempt a token refresh on this one, since refreshing an
 * insufficient-role token doesn't help.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        SecurityErrorResponses.write(objectMapper, response, HttpServletResponse.SC_FORBIDDEN,
                ErrorCode.FORBIDDEN, "Access denied");
    }
}
