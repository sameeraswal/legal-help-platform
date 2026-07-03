package com.legalhelp.common.exception;

public record ErrorResponse(String code, String message, String traceId) {
}
