package com.legalhelp.common.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.legalhelp.common.llm.LlmClient;
import com.legalhelp.common.llm.LlmException;
import com.legalhelp.common.llm.LlmHttpException;
import com.legalhelp.common.llm.LlmMessage;
import com.legalhelp.common.llm.LlmRole;
import com.legalhelp.common.llm.LlmStreamHandler;
import com.legalhelp.common.llm.openai.dto.OpenAiMessage;
import com.legalhelp.common.llm.openai.dto.OpenAiRequest;
import com.legalhelp.common.llm.openai.dto.OpenAiResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link LlmClient} implementation that talks to <em>any</em> OpenAI-compatible
 * {@code /chat/completions} endpoint — OpenAI, Groq, OpenRouter, Ollama, LM Studio, Together AI,
 * DeepInfra, or anything else implementing the same wire format. The provider is selected purely
 * through {@code app.llm.*} configuration ({@link OpenAiConfigurationProperties}); this class
 * contains no provider-specific branching except the optional OpenRouter attribution headers
 * described in {@link OpenAiConfigurationProperties.OpenRouter}.
 *
 * <p>Built directly on {@link HttpClient} (JDK 11+ built-in) — no OpenAI SDK dependency. A single
 * instance of this class is created as a Spring singleton bean and holds a single, shared
 * {@link HttpClient}; since the JDK client keeps HTTP connections alive and pools them per
 * destination for the lifetime of the client instance, this reuse is what provides connection
 * pooling here. The client's {@link HttpClient.Builder#executor(java.util.concurrent.Executor)}
 * uses a virtual-thread-per-task executor, which scales to high concurrent request volume without
 * needing to size a fixed thread pool.
 *
 * <p><b>Retries:</b> {@link #generate} retries the whole request/response exchange through
 * {@link RetryExecutor} on a retryable {@link LlmHttpException} (429, 502, 503, 504), with
 * exponential backoff. {@link #streamChat} retries only the initial connect-and-read-headers step
 * the same way; once any part of the response body has been streamed to the caller's
 * {@link LlmStreamHandler}, the exchange is no longer retried — replaying already-delivered
 * tokens would corrupt the caller's view of the conversation. Streaming failures after that point
 * are reported via {@link LlmStreamHandler#onError}.
 *
 * <p><b>Logging:</b> only request duration, provider, model, and (on failure) HTTP status codes
 * are logged. Prompts, conversation history, and the API key are never logged.
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleLlmClient.class);

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final String OPENROUTER_PROVIDER = "openrouter";

    private final OpenAiConfigurationProperties config;
    private final URI chatCompletionsUri;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService httpExecutor;
    private final RetryExecutor retryExecutor;
    private final StreamingEventParser streamingEventParser;

    public OpenAiCompatibleLlmClient(OpenAiConfigurationProperties config) {
        this.config = config;
        this.chatCompletionsUri = resolveChatCompletionsUri(config.baseUrl());
        this.objectMapper = buildObjectMapper();
        this.httpExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .executor(httpExecutor)
                .build();
        OpenAiConfigurationProperties.Retry retry = config.retry();
        this.retryExecutor = new RetryExecutor(retry.maxAttempts(), retry.initialBackoff(), retry.backoffMultiplier());
        this.streamingEventParser = new StreamingEventParser();
    }

    @Override
    public String generate(String systemPrompt, List<LlmMessage> history, String userPrompt) {
        long startNanos = System.nanoTime();
        HttpRequest request = buildRequest(systemPrompt, history, userPrompt, false);
        try {
            HttpResponse<String> response = retryExecutor.execute(() -> sendForString(request));
            OpenAiResponse parsed = parseJson(response.body(), OpenAiResponse.class);
            String content = parsed.firstContentOrNull();
            if (content == null || content.isBlank()) {
                throw new LlmException("LLM returned no text content, finish_reason=" + parsed.firstFinishReasonOrNull());
            }
            return content;
        } finally {
            logDuration("generate", startNanos);
        }
    }

    @Override
    public void streamChat(String systemPrompt, List<LlmMessage> history, String userPrompt, LlmStreamHandler handler) {
        long startNanos = System.nanoTime();
        HttpRequest request = buildRequest(systemPrompt, history, userPrompt, true);
        try {
            HttpResponse<Stream<String>> response = retryExecutor.execute(() -> sendForLines(request));
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> processStreamLine(line, handler));
            }
            handler.onComplete();
        } catch (Exception e) {
            log.error("OpenAI-compatible streamChat() call failed: {}", e.getMessage());
            handler.onError(e);
        } finally {
            logDuration("streamChat", startNanos);
        }
    }

    // ---------------------------------------------------------------------
    // Request building
    // ---------------------------------------------------------------------

    private HttpRequest buildRequest(String systemPrompt, List<LlmMessage> history, String userPrompt, boolean stream) {
        List<OpenAiMessage> messages = buildMessages(systemPrompt, history, userPrompt);
        OpenAiRequest body = new OpenAiRequest(config.model(), messages, config.temperature(), stream);
        String json = writeJson(body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(chatCompletionsUri)
                .timeout(config.requestTimeout())
                .header("Authorization", "Bearer " + config.apiKeyOrEmpty())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        addOpenRouterHeaders(builder);
        return builder.build();
    }

    private List<OpenAiMessage> buildMessages(String systemPrompt, List<LlmMessage> history, String userPrompt) {
        List<OpenAiMessage> messages = new ArrayList<>(history.size() + 2);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new OpenAiMessage("system", systemPrompt));
        }
        for (LlmMessage message : history) {
            messages.add(new OpenAiMessage(toWireRole(message.role()), message.content()));
        }
        messages.add(new OpenAiMessage("user", userPrompt));
        return messages;
    }

    private static String toWireRole(LlmRole role) {
        return role == LlmRole.ASSISTANT ? "assistant" : "user";
    }

    private void addOpenRouterHeaders(HttpRequest.Builder builder) {
        if (!OPENROUTER_PROVIDER.equalsIgnoreCase(config.provider())) {
            return;
        }
        String referer = config.openRouter().referer();
        if (referer != null && !referer.isBlank()) {
            builder.header("HTTP-Referer", referer);
        }
        String title = config.openRouter().title();
        if (title != null && !title.isBlank()) {
            builder.header("X-Title", title);
        }
    }

    private static URI resolveChatCompletionsUri(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalized + CHAT_COMPLETIONS_PATH);
    }

    // ---------------------------------------------------------------------
    // HTTP execution
    // ---------------------------------------------------------------------

    private HttpResponse<String> sendForString(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (!isSuccessful(response.statusCode())) {
                throw httpFailure(response.statusCode(), response.body());
            }
            return response;
        } catch (IOException e) {
            throw new LlmException("LLM request failed: I/O error contacting the LLM endpoint");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM request interrupted");
        }
    }

    private HttpResponse<Stream<String>> sendForLines(HttpRequest request) {
        try {
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (isSuccessful(response.statusCode())) {
                return response;
            }
            String body;
            try (Stream<String> errorLines = response.body()) {
                body = errorLines.collect(Collectors.joining("\n"));
            }
            throw httpFailure(response.statusCode(), body);
        } catch (IOException e) {
            throw new LlmException("LLM request failed: I/O error contacting the LLM endpoint");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM request interrupted");
        }
    }

    private static boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private LlmHttpException httpFailure(int statusCode, String body) {
        return new LlmHttpException(statusCode, "LLM endpoint returned HTTP " + statusCode + ": " + extractErrorMessage(body));
    }

    /** Best-effort extraction of {@code error.message} from a provider error body; never throws. */
    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "no response body";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.path("error").path("message");
            if (message.isTextual()) {
                return message.asText();
            }
        } catch (IOException ignored) {
            // Not JSON, or an unexpected shape — fall through to the generic message below.
        }
        return "unrecognized error response";
    }

    // ---------------------------------------------------------------------
    // Streaming
    // ---------------------------------------------------------------------

    private void processStreamLine(String line, LlmStreamHandler handler) {
        streamingEventParser.extractDataPayload(line).ifPresent(payload -> {
            OpenAiResponse chunk = parseJson(payload, OpenAiResponse.class);
            String token = chunk.firstContentOrNull();
            if (token != null && !token.isEmpty()) {
                handler.onToken(token);
            }
        });
    }

    // ---------------------------------------------------------------------
    // JSON
    // ---------------------------------------------------------------------

    private static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new LlmException("Failed to serialize LLM request body");
        }
    }

    private <T> T parseJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new LlmException("Malformed JSON response from LLM endpoint");
        }
    }

    // ---------------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------------

    private void logDuration(String operation, long startNanos) {
        long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        log.info("LLM {} completed in {} ms (provider={}, model={})", operation, durationMs, config.provider(), config.model());
    }

    @PreDestroy
    void shutdown() {
        httpExecutor.shutdown();
        try {
            if (!httpExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                httpExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            httpExecutor.shutdownNow();
        }
    }
}
