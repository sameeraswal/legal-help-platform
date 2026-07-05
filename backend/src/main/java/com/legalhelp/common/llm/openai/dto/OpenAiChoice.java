package com.legalhelp.common.llm.openai.dto;

/**
 * A single entry in {@code choices[]} on a chat-completion response.
 *
 * <p>Non-streaming responses populate {@link #message()}; streaming chunks ({@code stream=true})
 * populate {@link #delta()} instead. Exactly one of the two is non-null for a given payload, so
 * callers must check {@code stream} mode (or null-check both) rather than assuming one is set.
 *
 * <p>Deserialized with a snake_case-aware {@code ObjectMapper}, so {@code finish_reason} on the
 * wire maps to {@link #finishReason()} here without a {@code @JsonProperty} annotation.
 *
 * @param index        the choice index (always {@code 0} for the single-completion requests this
 *                     client issues)
 * @param message      the full message, present on non-streaming responses (nullable)
 * @param delta        the incremental delta, present on streaming chunks (nullable)
 * @param finishReason why generation stopped, present on the final chunk/response (nullable)
 */
public record OpenAiChoice(int index, OpenAiMessage message, OpenAiDelta delta, String finishReason) {
}
