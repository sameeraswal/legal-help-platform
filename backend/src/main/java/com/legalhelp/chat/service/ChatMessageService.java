package com.legalhelp.chat.service;

import com.legalhelp.chat.dto.ChatMessageResponse;
import com.legalhelp.chat.entity.ChatMessage;
import com.legalhelp.chat.entity.MessageSender;
import com.legalhelp.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository repository;

    public ChatMessageService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ChatMessage append(Long sessionId, MessageSender sender, String content) {
        return repository.save(new ChatMessage(sessionId, sender, content));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(Long sessionId) {
        return repository.findBySessionIdOrderByTimestampAsc(sessionId).stream().map(this::toResponse).toList();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getSessionId(), message.getSender(),
                message.getContent(), message.getTimestamp());
    }
}
