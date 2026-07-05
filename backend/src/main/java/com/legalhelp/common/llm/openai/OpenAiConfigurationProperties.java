package com.legalhelp.common.llm.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Binds the {@code app.llm.*} configuration block. This is the single point of variation between
 * OpenAI-compatible providers (OpenAI, Groq, OpenRouter, Ollama, LM Studio, Together AI,
 * DeepInfra, ...) — switching providers is a matter of changing {@link #baseUrl()},
 * {@link #model()} and {@link #apiKey()} in {@code application.yml} (or the corresponding
 * environment variables); no Java code changes.
 *
 * <p>Every optional field is null-safe: a compact constructor fills in a sensible default for
 * anything left unset, so {@link OpenAiCompatibleLlmClient} never has to null-check configuration
 * at call time. Nested records ({@link Retry}, {@link OpenRouter}) receive the same treatment,
 * recursively, so {@code app.llm.retry} and {@code app.llm.open-router} may be omitted entirely.
 *
 * @param provider    free-text provider label (e.g. {@code openai}, {@code groq}, {@code
 *                    openrouter}, {@code ollama}); used only to decide whether to attach the
 *                    OpenRouter-specific headers, never to branch on request/response shape
 * @param baseUrl     the provider's OpenAI-compatible base URL, e.g. {@code
 *                    https://api.openai.com/v1} — {@code /chat/completions} is appended to this
 * @param apiKey      bearer token sent as {@code Authorization: Bearer <apiKey>}; may be any
 *                    non-null placeholder (e.g. {@code "dummy"}) for providers that don't check it,
 *                    such as a local Ollama instance
 * @param model       the provider-specific model identifier, e.g. {@code gpt-4.1}, {@code
 *                    llama-3.3-70b-versatile}, {@code qwen3:8b}
 * @param temperature sampling temperature sent on every request; defaults to {@code 0.3}
 * @param connectTimeout timeout for establishing the TCP/TLS connection; defaults to 10 seconds
 * @param requestTimeout timeout for receiving the response (the whole exchange, streaming
 *                       included); defaults to 60 seconds
 * @param retry       retry/backoff tuning; defaults applied field-by-field, see {@link Retry}
 * @param openRouter  OpenRouter-specific attribution headers; defaults applied field-by-field,
 *                    see {@link OpenRouter}
 */
@Validated
@ConfigurationProperties(prefix = "app.llm")
public record OpenAiConfigurationProperties(
        String provider,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        Double temperature,
        Duration connectTimeout,
        Duration requestTimeout,
        Retry retry,
        OpenRouter openRouter) {

    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    public OpenAiConfigurationProperties {
        temperature = temperature == null ? DEFAULT_TEMPERATURE : temperature;
        connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        requestTimeout = requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout;
        retry = retry == null ? new Retry(null, null, null) : retry;
        openRouter = openRouter == null ? new OpenRouter(null, null) : openRouter;
    }

    /** @return the API key, or an empty string if unset (never {@code null}). */
    public String apiKeyOrEmpty() {
        return apiKey == null ? "" : apiKey;
    }

    /**
     * Retry/backoff tuning for transient upstream failures (HTTP 429, 502, 503, 504).
     *
     * @param maxAttempts        total attempts including the first, non-retry attempt; defaults
     *                           to {@code 3}
     * @param initialBackoff     delay before the first retry; defaults to {@code 500ms}
     * @param backoffMultiplier  multiplier applied to the backoff after each retry (exponential
     *                           backoff); defaults to {@code 2.0}
     */
    public record Retry(Integer maxAttempts, Duration initialBackoff, Double backoffMultiplier) {

        private static final int DEFAULT_MAX_ATTEMPTS = 3;
        private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(500);
        private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

        public Retry {
            maxAttempts = maxAttempts == null ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
            initialBackoff = initialBackoff == null ? DEFAULT_INITIAL_BACKOFF : initialBackoff;
            backoffMultiplier = backoffMultiplier == null ? DEFAULT_BACKOFF_MULTIPLIER : backoffMultiplier;
        }
    }

    /**
     * Optional attribution headers recommended by OpenRouter (see
     * <a href="https://openrouter.ai/docs">openrouter.ai/docs</a>). Ignored for every other
     * provider, and skipped even for OpenRouter when left blank.
     *
     * @param referer sent as the {@code HTTP-Referer} header when non-blank
     * @param title   sent as the {@code X-Title} header when non-blank
     */
    public record OpenRouter(String referer, String title) {
    }
}
