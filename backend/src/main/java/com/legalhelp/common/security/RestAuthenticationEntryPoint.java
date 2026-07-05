package com.legalhelp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalhelp.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without an explicit {@link AuthenticationEntryPoint}, Spring Security's default for a
 * stateless API (no {@code httpBasic()}/{@code formLogin()}) is {@code Http403ForbiddenEntryPoint}
 * — every unauthenticated request (missing, malformed, or expired token) comes back as a bare
 * 403 with no body. That makes it impossible for a client to tell "your session expired, try
 * refreshing" apart from "you don't have permission to do that" (an authenticated-but-wrong-role
 * request, handled by {@link RestAccessDeniedHandler}). This entry point returns a proper 401
 * with the same {@code {code, message, traceId}} shape as {@code GlobalExceptionHandler}, so the
 * frontend can react to 401 specifically by attempting a token refresh.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        SecurityErrorResponses.write(objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED, "Authentication required");
    }
}
