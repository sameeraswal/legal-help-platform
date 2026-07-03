package com.legalhelp.common.llm;

/** Callback for a streamed LLM chat response. Implementations must be safe to call from a non-request thread. */
public interface LlmStreamHandler {
    void onToken(String textDelta);

    void onComplete();

    void onError(Throwable error);
}
