package com.legalhelp.chat.service;

import com.legalhelp.chat.dto.ChatSessionResponse;
import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.entity.CounterpartType;
import com.legalhelp.chat.entity.MessageSender;
import com.legalhelp.chat.repository.ChatSessionRepository;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatSessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageService chatMessageService;

    public ChatSessionService(ChatSessionRepository sessionRepository, ChatMessageService chatMessageService) {
        this.sessionRepository = sessionRepository;
        this.chatMessageService = chatMessageService;
    }

    @Transactional
    public ChatSessionResponse startLlmSession(Long customerId) {
        requireNoActiveSession(customerId);
        ChatSession session = sessionRepository.save(new ChatSession(customerId, CounterpartType.LLM, null));
        return toResponse(session);
    }

    @Transactional
    public ChatSessionResponse startLawyerSession(Long customerId, Long lawyerId) {
        requireNoActiveSession(customerId);
        ChatSession session = sessionRepository.save(new ChatSession(customerId, CounterpartType.LAWYER, lawyerId));
        chatMessageService.append(session.getId(), MessageSender.SYSTEM, "Session started.");
        return toResponse(session);
    }

    @Transactional
    public void endSession(Long sessionId, String reason) {
        ChatSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (session.getStatus() == ChatSessionStatus.ACTIVE) {
            session.end();
            chatMessageService.append(sessionId, MessageSender.SYSTEM, "Session ended: " + reason);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ChatSessionResponse> activeSessionFor(Long customerId) {
        return sessionRepository.findByCustomerIdAndStatus(customerId, ChatSessionStatus.ACTIVE).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ChatSession getOwnedEntity(Long customerId, Long lawyerId, Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        boolean isCustomer = customerId != null && customerId.equals(session.getCustomerId());
        boolean isLawyer = lawyerId != null && lawyerId.equals(session.getLawyerId());
        if (!isCustomer && !isLawyer) {
            throw new AccessDeniedException("This chat session does not belong to the current user");
        }
        return session;
    }

    @Transactional(readOnly = true)
    public List<ChatSession> activeSessions() {
        return sessionRepository.findByStatus(ChatSessionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> myCustomerSessions(Long customerId) {
        return sessionRepository.findByCustomerIdOrderByStartedAtDesc(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> myLawyerSessions(Long lawyerId) {
        return sessionRepository.findByLawyerIdOrderByStartedAtDesc(lawyerId).stream().map(this::toResponse).toList();
    }

    private void requireNoActiveSession(Long customerId) {
        if (sessionRepository.findByCustomerIdAndStatus(customerId, ChatSessionStatus.ACTIVE).isPresent()) {
            throw new BadRequestException("You already have an active chat session");
        }
    }

    ChatSessionResponse toResponse(ChatSession session) {
        return new ChatSessionResponse(session.getId(), session.getCustomerId(), session.getCounterpartType(),
                session.getLawyerId(), session.getStatus(), session.getBilledSeconds(), session.getStartedAt(),
                session.getEndedAt());
    }
}
