package com.legalhelp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalhelp.common.exception.ErrorCode;
import com.legalhelp.common.exception.ErrorResponse;
import com.legalhelp.common.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Shared JSON error body for the security-filter-level handlers ({@link RestAuthenticationEntryPoint},
 * {@link RestAccessDeniedHandler}). These run before the DispatcherServlet, so they can't go
 * through {@code GlobalExceptionHandler}'s {@code @ExceptionHandler} methods — this writes the
 * same {@link ErrorResponse} shape directly.
 */
final class SecurityErrorResponses {

    private SecurityErrorResponses() {
    }

    static void write(ObjectMapper objectMapper, HttpServletResponse response, int status, ErrorCode code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code.name(), message, traceId != null ? traceId : "unknown"));
    }
}
