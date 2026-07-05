package com.legalhelp.common.llm.openai.dto;

/**
 * A single chat message in the OpenAI-compatible {@code /chat/completions} wire format.
 *
 * @param role    one of {@code "system"}, {@code "user"}, {@code "assistant"}
 * @param content the message text
 */
public record OpenAiMessage(String role, String content) {
}
