package com.legalhelp.common.llm.openai.dto;

import java.util.List;

/**
 * Request body for {@code POST /chat/completions} on any OpenAI-compatible endpoint
 * (OpenAI, Groq, OpenRouter, Ollama, LM Studio, Together AI, DeepInfra, ...).
 *
 * @param model       the provider-specific model identifier
 * @param messages    the conversation, in order (system, then history, then the new user turn)
 * @param temperature sampling temperature
 * @param stream      {@code true} to request a server-sent-events streaming response
 */
public record OpenAiRequest(String model, List<OpenAiMessage> messages, double temperature, boolean stream) {
}
