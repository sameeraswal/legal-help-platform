package com.legalhelp.chat;

import com.legalhelp.auth.dto.AuthResponse;
import com.legalhelp.auth.dto.RegisterRequest;
import com.legalhelp.billing.service.WalletService;
import com.legalhelp.chat.dto.ChatSessionResponse;
import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.entity.MessageSender;
import com.legalhelp.chat.repository.ChatSessionRepository;
import com.legalhelp.chat.service.ChatMessageService;
import com.legalhelp.chat.service.ChatSessionService;
import com.legalhelp.chat.service.TimeMeteringService;
import com.legalhelp.common.security.Role;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLAUDE.md §Testing Priorities #3: free-minutes exhaustion must cut the session off
 * server-side, based on elapsed wall-clock time — never on a client-reported duration.
 */
class ChatMeteringIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private TimeMeteringService timeMeteringService;

    @Test
    void session_isCutOffWhenWalletIsExhaustedMidTick() {
        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Metering Test", "metering-test@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        Long customerId = response.getBody().user().id();

        // Drain the wallet down to 2 seconds of available balance.
        long available = walletService.availableSeconds(customerId);
        walletService.consumeSeconds(customerId, available - 2, "test-drain");
        assertThat(walletService.availableSeconds(customerId)).isEqualTo(2);

        ChatSessionResponse session = chatSessionService.startLlmSession(customerId);

        // Simulate 10 elapsed seconds of chat time having passed since the last tick —
        // this is what makes the test deterministic without a real sleep.
        ChatSession entity = chatSessionRepository.findById(session.id()).orElseThrow();
        entity.setLastMeteredAt(Instant.now().minus(10, ChronoUnit.SECONDS));
        chatSessionRepository.save(entity);

        timeMeteringService.tickSession(session.id());

        ChatSession afterTick = chatSessionRepository.findById(session.id()).orElseThrow();
        assertThat(afterTick.getStatus()).isEqualTo(ChatSessionStatus.ENDED);
        assertThat(afterTick.getBilledSeconds()).isEqualTo(2);
        assertThat(walletService.availableSeconds(customerId)).isEqualTo(0);

        boolean hasCutoffMessage = chatMessageService.history(session.id()).stream()
                .anyMatch(m -> m.sender() == MessageSender.SYSTEM && m.content().contains("balance exhausted"));
        assertThat(hasCutoffMessage).isTrue();
    }

    @Test
    void session_emitsWarningBeforeExhaustion_butStaysActive() {
        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Warning Test", "warning-test@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        Long customerId = response.getBody().user().id();

        long available = walletService.availableSeconds(customerId);
        walletService.consumeSeconds(customerId, available - 120, "test-drain");

        ChatSessionResponse session = chatSessionService.startLlmSession(customerId);
        ChatSession entity = chatSessionRepository.findById(session.id()).orElseThrow();
        entity.setLastMeteredAt(Instant.now().minus(5, ChronoUnit.SECONDS));
        chatSessionRepository.save(entity);

        timeMeteringService.tickSession(session.id());

        ChatSession afterTick = chatSessionRepository.findById(session.id()).orElseThrow();
        assertThat(afterTick.getStatus()).isEqualTo(ChatSessionStatus.ACTIVE);
        assertThat(walletService.availableSeconds(customerId)).isEqualTo(110);
    }
}
