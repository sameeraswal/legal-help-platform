package com.legalhelp.chat.service;

import com.legalhelp.chat.dto.ChatEvent;
import com.legalhelp.chat.dto.OutgoingEventType;
import com.legalhelp.chat.entity.ChatMessage;
import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.entity.CounterpartType;
import com.legalhelp.chat.entity.MessageSender;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.llm.LlmClient;
import com.legalhelp.common.llm.LlmMessage;
import com.legalhelp.common.llm.LlmRole;
import com.legalhelp.common.llm.LlmStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Bridges a customer's chat message to the streamed LLM response over STOMP.
 * Streaming runs on a virtual thread so it never blocks the WebSocket inbound
 * channel thread pool.
 */
@Service
public class LlmChatService {

    private static final Logger log = LoggerFactory.getLogger(LlmChatService.class);

    private static final String SYSTEM_PROMPT = """
            You are a legal information assistant for an Indian online legal help platform. Answer questions \
            in plain, accessible language. You may explain general legal concepts, procedures, and relevant \
            laws, but you must NOT present your answers as formal legal advice, and you must recommend the \
            user consult a qualified lawyer (available on this platform) for advice specific to their situation. \
            Keep responses concise and focused on the user's question.""";

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final LlmClient llmClient;
    private final SimpMessagingTemplate messagingTemplate;

    public LlmChatService(ChatSessionService chatSessionService, ChatMessageService chatMessageService,
                           LlmClient llmClient, SimpMessagingTemplate messagingTemplate) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.llmClient = llmClient;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void handleUserMessage(Long customerId, Long sessionId, String content) {
        ChatSession session = chatSessionService.getOwnedEntity(customerId, null, sessionId);
        if (session.getStatus() != ChatSessionStatus.ACTIVE) {
            throw new BadRequestException("This chat session has ended");
        }
        if (session.getCounterpartType() != CounterpartType.LLM) {
            throw new BadRequestException("This session is not an LLM chat session");
        }

        List<LlmMessage> history = chatMessageService.history(sessionId).stream()
                .filter(m -> m.sender() == MessageSender.CUSTOMER || m.sender() == MessageSender.LLM)
                .map(m -> new LlmMessage(m.sender() == MessageSender.CUSTOMER ? LlmRole.USER : LlmRole.ASSISTANT, m.content()))
                .toList();

        chatMessageService.append(sessionId, MessageSender.CUSTOMER, content);

        Thread.ofVirtual().name("llm-chat-" + sessionId).start(() -> streamResponse(sessionId, history, content));
    }

    private void streamResponse(Long sessionId, List<LlmMessage> history, String userPrompt) {
        String topic = "/topic/sessions/" + sessionId;
        StringBuilder fullResponse = new StringBuilder();

        llmClient.streamChat(SYSTEM_PROMPT, history, userPrompt, new LlmStreamHandler() {
            @Override
            public void onToken(String textDelta) {
                fullResponse.append(textDelta);
                messagingTemplate.convertAndSend(topic, ChatEvent.of(OutgoingEventType.TOKEN_DELTA, textDelta));
            }

            @Override
            public void onComplete() {
                ChatMessage saved = chatMessageService.append(sessionId, MessageSender.LLM, fullResponse.toString());
                messagingTemplate.convertAndSend(topic, ChatEvent.of(OutgoingEventType.TOKEN_COMPLETE,
                        Map.of("messageId", saved.getId())));
            }

            @Override
            public void onError(Throwable error) {
                log.error("LLM streaming failed for session {}", sessionId, error);
                messagingTemplate.convertAndSend(topic, ChatEvent.of(OutgoingEventType.ERROR, "The assistant is temporarily unavailable. Please try again."));
            }
        });
    }
}
