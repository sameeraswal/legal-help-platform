package com.legalhelp.common.llm.openai.dto;

import java.util.List;

/**
 * Response envelope for {@code POST /chat/completions}. The same shape is reused for both the
 * single non-streaming response body and each {@code data: {...}} chunk of a streaming response
 * ({@code object} is {@code "chat.completion"} vs {@code "chat.completion.chunk"} respectively) —
 * this client only reads {@link #choices()} either way.
 *
 * <p>Unknown fields (e.g. {@code usage}, {@code created}, {@code system_fingerprint}) are ignored
 * by the configured {@code ObjectMapper}; they are not needed here.
 *
 * @param id      the provider-assigned response/completion id (nullable, unused, kept for
 *                debugging/log correlation if ever needed)
 * @param model   the model that served the request (nullable)
 * @param choices the completion choices; expected to contain exactly one entry for the requests
 *                this client issues
 */
public record OpenAiResponse(String id, String model, List<OpenAiChoice> choices) {

    /**
     * @return the first choice's content, from {@code message.content} (non-streaming) or
     *         {@code delta.content} (streaming), or {@code null} if no content is present on
     *         this response/chunk.
     */
    public String firstContentOrNull() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        OpenAiChoice choice = choices.get(0);
        if (choice.message() != null) {
            return choice.message().content();
        }
        if (choice.delta() != null) {
            return choice.delta().content();
        }
        return null;
    }

    /**
     * @return the first choice's {@code finish_reason}, or {@code null} if there is no choice or
     *         no finish reason was reported yet.
     */
    public String firstFinishReasonOrNull() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).finishReason();
    }
}
