package com.legalhelp.common.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Anthropic Claude implementation of {@link LlmClient}. Petition drafting runs with
 * adaptive thinking for higher-quality structured output; chat streaming runs without
 * thinking to keep first-token latency low (target < 3s per the NFRs).
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);
    private static final long DRAFT_MAX_TOKENS = 8000L;
    private static final long CHAT_MAX_TOKENS = 2048L;

    private final AnthropicClient client;
    private final Model model;

    public AnthropicLlmClient(@Value("${app.llm.api-key}") String apiKey,
                               @Value("${app.llm.model}") String modelId) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = Model.of(modelId);
    }

    @Override
    public String generate(String systemPrompt, List<LlmMessage> history, String userPrompt) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(DRAFT_MAX_TOKENS)
                .system(systemPrompt)
                .thinking(ThinkingConfigAdaptive.builder().build());
        appendHistory(builder, history);
        builder.addUserMessage(userPrompt);

        Message response;
        try {
            response = client.messages().create(builder.build());
        } catch (Exception e) {
            log.error("Anthropic generate() call failed", e);
            throw new LlmException("LLM request failed: " + e.getMessage());
        }

        StringBuilder text = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> text.append(t.text()));
        }
        if (text.isEmpty()) {
            throw new LlmException("LLM returned no text content, stop_reason=" + response.stopReason());
        }
        return text.toString();
    }

    @Override
    public void streamChat(String systemPrompt, List<LlmMessage> history, String userPrompt, LlmStreamHandler handler) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(CHAT_MAX_TOKENS)
                .system(systemPrompt);
        appendHistory(builder, history);
        builder.addUserMessage(userPrompt);

        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(builder.build())) {
            stream.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(delta -> delta.delta().text().stream())
                    .forEach(textDelta -> handler.onToken(textDelta.text()));
            handler.onComplete();
        } catch (Exception e) {
            log.error("Anthropic streamChat() call failed", e);
            handler.onError(e);
        }
    }

    private void appendHistory(MessageCreateParams.Builder builder, List<LlmMessage> history) {
        for (LlmMessage message : history) {
            if (message.role() == LlmRole.USER) {
                builder.addUserMessage(message.content());
            } else {
                builder.addAssistantMessage(message.content());
            }
        }
    }
}
