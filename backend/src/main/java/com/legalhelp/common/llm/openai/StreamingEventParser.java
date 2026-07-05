package com.legalhelp.common.llm.openai;

import java.util.Optional;

/**
 * Parses individual lines of an OpenAI-compatible {@code text/event-stream} response body
 * (Server-Sent Events). Line-oriented and stateless: {@code OpenAiCompatibleLlmClient} feeds it
 * one line at a time as the HTTP client reads the response body, so no full-response buffering
 * is ever required.
 *
 * <p>Every provider covered by this client (OpenAI, Groq, OpenRouter, Ollama, LM Studio, Together
 * AI, DeepInfra) frames streamed chunks identically: each event is a line of the form
 * {@code data: {"...json..."}}, blank lines separate events, and the stream ends with a literal
 * {@code data: [DONE]} line just before the connection closes.
 */
public final class StreamingEventParser {

    private static final String DATA_PREFIX = "data:";
    private static final String DONE_PAYLOAD = "[DONE]";

    /**
     * @param rawLine one line of the response body, as read by the HTTP client (may be blank, a
     *                comment, or any other non-data SSE line)
     * @return the raw JSON payload of a {@code data: ...} line, or {@link Optional#empty()} if
     *         {@code rawLine} is not a data line (including the terminal {@code data: [DONE]}
     *         line, which is intentionally excluded — callers should not attempt to parse it)
     */
    public Optional<String> extractDataPayload(String rawLine) {
        if (rawLine == null) {
            return Optional.empty();
        }
        String trimmed = rawLine.strip();
        if (!trimmed.startsWith(DATA_PREFIX)) {
            return Optional.empty();
        }
        String payload = trimmed.substring(DATA_PREFIX.length()).strip();
        if (payload.isEmpty() || isDone(payload)) {
            return Optional.empty();
        }
        return Optional.of(payload);
    }

    /** @return {@code true} if {@code payload} is the {@code [DONE]} stream-termination sentinel. */
    public boolean isDone(String payload) {
        return DONE_PAYLOAD.equals(payload);
    }
}
