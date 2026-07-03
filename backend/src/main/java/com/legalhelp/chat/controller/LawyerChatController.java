package com.legalhelp.chat.controller;

import com.legalhelp.chat.dto.ChatMessageResponse;
import com.legalhelp.chat.dto.ChatSessionResponse;
import com.legalhelp.chat.entity.ChatSessionStatus;
import com.legalhelp.chat.service.ChatMessageService;
import com.legalhelp.chat.service.ChatSessionService;
import com.legalhelp.chat.service.PresenceService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lawyer/chat")
@PreAuthorize("hasRole('LAWYER')")
public class LawyerChatController {

    private final PresenceService presenceService;
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    public LawyerChatController(PresenceService presenceService, ChatSessionService chatSessionService,
                                 ChatMessageService chatMessageService) {
        this.presenceService = presenceService;
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/sessions")
    public List<ChatSessionResponse> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return chatSessionService.myLawyerSessions(principal.userId());
    }

    @GetMapping("/sessions/active")
    public Optional<ChatSessionResponse> active(@AuthenticationPrincipal AuthPrincipal principal) {
        return chatSessionService.myLawyerSessions(principal.userId()).stream()
                .filter(s -> s.status() == ChatSessionStatus.ACTIVE)
                .findFirst();
    }

    @PostMapping("/presence/online")
    public void goOnline(@AuthenticationPrincipal AuthPrincipal principal) {
        presenceService.setOnline(principal.userId());
    }

    @PostMapping("/presence/offline")
    public void goOffline(@AuthenticationPrincipal AuthPrincipal principal) {
        presenceService.setOffline(principal.userId());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> history(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long sessionId) {
        chatSessionService.getOwnedEntity(null, principal.userId(), sessionId);
        return chatMessageService.history(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/end")
    public void end(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long sessionId) {
        chatSessionService.getOwnedEntity(null, principal.userId(), sessionId);
        chatSessionService.endSession(sessionId, "lawyer ended the session");
    }
}
