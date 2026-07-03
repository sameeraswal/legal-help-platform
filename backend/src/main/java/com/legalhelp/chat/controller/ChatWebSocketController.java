package com.legalhelp.chat.controller;

import com.legalhelp.chat.dto.WsIncomingMessage;
import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.CounterpartType;
import com.legalhelp.chat.repository.ChatSessionRepository;
import com.legalhelp.chat.service.LawyerChatService;
import com.legalhelp.chat.service.LlmChatService;
import com.legalhelp.chat.websocket.StompPrincipal;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.Role;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ChatSessionRepository chatSessionRepository;
    private final LlmChatService llmChatService;
    private final LawyerChatService lawyerChatService;

    public ChatWebSocketController(ChatSessionRepository chatSessionRepository, LlmChatService llmChatService,
                                    LawyerChatService lawyerChatService) {
        this.chatSessionRepository = chatSessionRepository;
        this.llmChatService = llmChatService;
        this.lawyerChatService = lawyerChatService;
    }

    @MessageMapping("/chat.send")
    public void send(WsIncomingMessage message, StompPrincipal principal) {
        ChatSession session = chatSessionRepository.findById(message.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        boolean senderIsLawyer = Role.LAWYER.name().equals(principal.authPrincipal().role());

        if (session.getCounterpartType() == CounterpartType.LLM) {
            llmChatService.handleUserMessage(principal.authPrincipal().userId(), message.sessionId(), message.content());
        } else {
            Long customerId = senderIsLawyer ? null : principal.authPrincipal().userId();
            Long lawyerId = senderIsLawyer ? principal.authPrincipal().userId() : null;
            lawyerChatService.relayMessage(customerId, lawyerId, message.sessionId(), message.content());
        }
    }
}
