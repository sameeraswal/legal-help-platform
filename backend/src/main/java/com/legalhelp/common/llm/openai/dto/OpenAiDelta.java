package com.legalhelp.common.llm.openai.dto;

/**
 * The {@code choices[].delta} object on a streamed ({@code stream=true}) chat-completion chunk.
 * Only {@code content} is populated on most tokens; {@code role} is typically only present on
 * the first chunk of a stream.
 *
 * @param role    the speaker role, present only on the first chunk (nullable)
 * @param content the incremental text for this chunk (nullable — some chunks carry no content)
 */
public record OpenAiDelta(String role, String content) {
}
