package com.legalhelp.common.llm;

import java.util.Set;

/**
 * Raised when an OpenAI-compatible endpoint returns a non-2xx HTTP status. Carries the raw
 * status code so callers (namely {@code RetryExecutor}) can decide whether the failure is
 * transient and worth retrying, without re-parsing the exception message.
 */
public class LlmHttpException extends LlmException {

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);

    private final int statusCode;

    public LlmHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    /** @return {@code true} if this status code (429, 502, 503, 504) warrants a retry. */
    public boolean isRetryable() {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }
}
