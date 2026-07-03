package com.legalhelp.common.llm;

import java.util.List;

/**
 * Provider-agnostic LLM abstraction. Feature modules (petition, chat) MUST depend
 * only on this interface — never call a provider SDK directly (see CLAUDE.md
 * domain rule #9). Swapping providers means adding a new implementation here.
 */
public interface LlmClient {

    /** Single-shot, non-streaming generation. Used for petition drafting. */
    String generate(String systemPrompt, List<LlmMessage> history, String userPrompt);

    /** Streamed generation for interactive chat. */
    void streamChat(String systemPrompt, List<LlmMessage> history, String userPrompt, LlmStreamHandler handler);
}
