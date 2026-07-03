package com.legalhelp.chat.service;

import com.legalhelp.billing.service.LawyerWalletService;
import com.legalhelp.billing.service.WalletService;
import com.legalhelp.chat.dto.ChatEvent;
import com.legalhelp.chat.dto.OutgoingEventType;
import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.entity.CounterpartType;
import com.legalhelp.chat.entity.MessageSender;
import com.legalhelp.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * The single source of truth for chat billing (CLAUDE.md domain rule #1): a scheduled
 * tick converts elapsed wall-clock time on every ACTIVE session into wallet consumption,
 * server-side. Nothing here trusts a client-reported duration. A session is cut off the
 * instant the customer's wallet can't cover the elapsed tick.
 */
@Service
public class TimeMeteringService {

    private static final Logger log = LoggerFactory.getLogger(TimeMeteringService.class);

    private final ChatSessionRepository sessionRepository;
    private final WalletService walletService;
    private final LawyerWalletService lawyerWalletService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final long warningThresholdSeconds;
    private final boolean scheduledMeteringEnabled;

    public TimeMeteringService(ChatSessionRepository sessionRepository, WalletService walletService,
                                LawyerWalletService lawyerWalletService, ChatMessageService chatMessageService,
                                SimpMessagingTemplate messagingTemplate,
                                @Value("${app.chat.cutoff-warning-seconds}") long warningThresholdSeconds,
                                @Value("${app.chat.scheduled-metering-enabled:true}") boolean scheduledMeteringEnabled) {
        this.sessionRepository = sessionRepository;
        this.walletService = walletService;
        this.lawyerWalletService = lawyerWalletService;
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
        this.warningThresholdSeconds = warningThresholdSeconds;
        this.scheduledMeteringEnabled = scheduledMeteringEnabled;
    }

    /** Disabled in tests (app.chat.scheduled-metering-enabled=false) so tests can call tickSession() deterministically. */
    @Scheduled(fixedDelayString = "${app.chat.metering-tick-seconds}000")
    public void meterActiveSessions() {
        if (!scheduledMeteringEnabled) {
            return;
        }
        for (ChatSession session : sessionRepository.findByStatus(ChatSessionStatus.ACTIVE)) {
            try {
                tickSession(session.getId());
            } catch (Exception e) {
                log.error("Metering tick failed for session {}", session.getId(), e);
            }
        }
    }

    @Transactional
    public void tickSession(Long sessionId) {
        ChatSession session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null || session.getStatus() != ChatSessionStatus.ACTIVE) {
            return;
        }

        long elapsed = Duration.between(session.getLastMeteredAt(), Instant.now()).getSeconds();
        if (elapsed <= 0) {
            return;
        }

        long available = walletService.availableSeconds(session.getCustomerId());
        long toConsume = Math.min(elapsed, available);

        if (toConsume > 0) {
            walletService.consumeSeconds(session.getCustomerId(), toConsume, "chat-session:" + sessionId);
            session.addBilledSeconds(toConsume);
            if (session.getCounterpartType() == CounterpartType.LAWYER) {
                lawyerWalletService.creditEarning(session.getLawyerId(), toConsume, "chat-session:" + sessionId);
            }
        }
        session.setLastMeteredAt(Instant.now());

        long remainingAfterTick = available - toConsume;
        String topic = "/topic/sessions/" + sessionId;

        if (toConsume < elapsed || remainingAfterTick <= 0) {
            session.end();
            chatMessageService.append(sessionId, MessageSender.SYSTEM, "Session ended: wallet balance exhausted.");
            messagingTemplate.convertAndSend(topic,
                    ChatEvent.of(OutgoingEventType.SESSION_ENDED, Map.of("reason", "BALANCE_EXHAUSTED")));
        } else if (remainingAfterTick <= warningThresholdSeconds) {
            messagingTemplate.convertAndSend(topic,
                    ChatEvent.of(OutgoingEventType.SESSION_WARNING, Map.of("remainingSeconds", remainingAfterTick)));
        }
    }
}
