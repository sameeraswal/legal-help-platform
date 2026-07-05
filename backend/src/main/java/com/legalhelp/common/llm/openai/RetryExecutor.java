package com.legalhelp.common.llm.openai;

import com.legalhelp.common.llm.LlmException;
import com.legalhelp.common.llm.LlmHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Retries a blocking action with exponential backoff when it fails with a retryable
 * {@link LlmHttpException} (HTTP 429, 502, 503, 504 — see {@link LlmHttpException#isRetryable()}).
 * Any other exception, or a retryable one on the final attempt, propagates to the caller
 * unchanged.
 *
 * <p>Stateless and thread-safe: all fields are immutable, so a single instance may be shared
 * across concurrent requests. Intended for the single request/response exchange in
 * {@code OpenAiCompatibleLlmClient#generate} and for the initial connect-and-read-headers step of
 * {@code OpenAiCompatibleLlmClient#streamChat} — never for retrying a partially-consumed stream,
 * which would risk replaying tokens already delivered to the caller's handler.
 */
public final class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final double backoffMultiplier;
    private final Consumer<Duration> sleeper;

    public RetryExecutor(int maxAttempts, Duration initialBackoff, double backoffMultiplier) {
        this(maxAttempts, initialBackoff, backoffMultiplier, RetryExecutor::sleepUninterruptibly);
    }

    /** Test-only constructor allowing the backoff delay to be observed without actually waiting. */
    RetryExecutor(int maxAttempts, Duration initialBackoff, double backoffMultiplier, Consumer<Duration> sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.backoffMultiplier = backoffMultiplier;
        this.sleeper = sleeper;
    }

    /**
     * Runs {@code action}, retrying on a retryable {@link LlmHttpException} up to
     * {@code maxAttempts} times in total, with exponential backoff between attempts.
     *
     * @param action the action to run; must be safe to call more than once
     * @return the action's result on the first successful attempt
     * @throws LlmHttpException the last failure, once retries are exhausted or the failure isn't
     *                          retryable
     */
    public <T> T execute(Supplier<T> action) {
        Duration backoff = initialBackoff;
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (LlmHttpException e) {
                if (!e.isRetryable() || attempt >= maxAttempts) {
                    throw e;
                }
                log.warn("Retryable LLM HTTP status {} on attempt {}/{}; backing off {} ms",
                        e.statusCode(), attempt, maxAttempts, backoff.toMillis());
                sleeper.accept(backoff);
                backoff = Duration.ofMillis((long) (backoff.toMillis() * backoffMultiplier));
            }
        }
    }

    private static void sleepUninterruptibly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Interrupted while backing off before an LLM request retry");
        }
    }
}
