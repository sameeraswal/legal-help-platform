package com.legalhelp.chat.service;

import com.legalhelp.chat.dto.ChatEvent;
import com.legalhelp.chat.dto.ChatMessageResponse;
import com.legalhelp.chat.dto.OutgoingEventType;
import com.legalhelp.chat.entity.ChatMessage;
import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.entity.CounterpartType;
import com.legalhelp.chat.entity.MessageSender;
import com.legalhelp.common.exception.BadRequestException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LawyerChatService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public LawyerChatService(ChatSessionService chatSessionService, ChatMessageService chatMessageService,
                              SimpMessagingTemplate messagingTemplate) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void relayMessage(Long customerId, Long lawyerId, Long sessionId, String content) {
        ChatSession session = chatSessionService.getOwnedEntity(customerId, lawyerId, sessionId);
        if (session.getStatus() != ChatSessionStatus.ACTIVE) {
            throw new BadRequestException("This chat session has ended");
        }
        if (session.getCounterpartType() != CounterpartType.LAWYER) {
            throw new BadRequestException("This session is not a lawyer chat session");
        }

        MessageSender sender = lawyerId != null ? MessageSender.LAWYER : MessageSender.CUSTOMER;
        ChatMessage saved = chatMessageService.append(sessionId, sender, content);
        ChatMessageResponse response = new ChatMessageResponse(saved.getId(), saved.getSessionId(), saved.getSender(),
                saved.getContent(), saved.getTimestamp());

        messagingTemplate.convertAndSend("/topic/sessions/" + sessionId, ChatEvent.of(OutgoingEventType.MESSAGE, response));
    }
}
