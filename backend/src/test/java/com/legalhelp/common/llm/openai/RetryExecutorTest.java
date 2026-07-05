package com.legalhelp.common.llm.openai;

import com.legalhelp.common.llm.LlmHttpException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryExecutorTest {

    @Test
    void returnsResultWithoutRetryingOnSuccess() {
        RetryExecutor executor = new RetryExecutor(3, Duration.ofMillis(10), 2.0, d -> { throw new AssertionError("should not sleep"); });
        AtomicInteger calls = new AtomicInteger();

        String result = executor.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void retriesOnRetryableStatusThenSucceeds() {
        List<Duration> sleeps = new ArrayList<>();
        RetryExecutor executor = new RetryExecutor(3, Duration.ofMillis(100), 2.0, sleeps::add);
        AtomicInteger calls = new AtomicInteger();

        String result = executor.execute(() -> {
            int attempt = calls.incrementAndGet();
            if (attempt < 3) {
                throw new LlmHttpException(429, "rate limited");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(3);
        assertThat(sleeps).containsExactly(Duration.ofMillis(100), Duration.ofMillis(200));
    }

    @Test
    void givesUpAfterMaxAttemptsAndRethrowsLastFailure() {
        RetryExecutor executor = new RetryExecutor(3, Duration.ofMillis(1), 2.0, d -> { });
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute(() -> {
            calls.incrementAndGet();
            throw new LlmHttpException(503, "unavailable");
        }))
                .isInstanceOf(LlmHttpException.class)
                .extracting(e -> ((LlmHttpException) e).statusCode())
                .isEqualTo(503);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryNonRetryableStatus() {
        RetryExecutor executor = new RetryExecutor(5, Duration.ofMillis(1), 2.0, d -> { throw new AssertionError("should not sleep"); });
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute(() -> {
            calls.incrementAndGet();
            throw new LlmHttpException(401, "unauthorized");
        }))
                .isInstanceOf(LlmHttpException.class)
                .extracting(e -> ((LlmHttpException) e).statusCode())
                .isEqualTo(401);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void appliesExponentialBackoffAcrossMultipleRetries() {
        List<Duration> sleeps = new ArrayList<>();
        RetryExecutor executor = new RetryExecutor(5, Duration.ofMillis(50), 3.0, sleeps::add);
        AtomicInteger calls = new AtomicInteger();

        executor.execute(() -> {
            int attempt = calls.incrementAndGet();
            if (attempt < 4) {
                throw new LlmHttpException(502, "bad gateway");
            }
            return "ok";
        });

        assertThat(sleeps).containsExactly(Duration.ofMillis(50), Duration.ofMillis(150), Duration.ofMillis(450));
    }

    @Test
    void rejectsNonPositiveMaxAttempts() {
        assertThatThrownBy(() -> new RetryExecutor(0, Duration.ofMillis(1), 2.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
