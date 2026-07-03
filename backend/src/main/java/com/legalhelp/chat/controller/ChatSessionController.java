package com.legalhelp.chat.controller;

import com.legalhelp.chat.dto.ChatMessageResponse;
import com.legalhelp.chat.dto.ChatSessionResponse;
import com.legalhelp.chat.dto.StartLawyerSessionRequest;
import com.legalhelp.chat.service.ChatMessageService;
import com.legalhelp.chat.service.ChatSessionService;
import com.legalhelp.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat/sessions")
@PreAuthorize("hasRole('CUSTOMER')")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    public ChatSessionController(ChatSessionService chatSessionService, ChatMessageService chatMessageService) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/active")
    public Optional<ChatSessionResponse> active(@AuthenticationPrincipal AuthPrincipal principal) {
        return chatSessionService.activeSessionFor(principal.userId());
    }

    @PostMapping("/llm")
    public ChatSessionResponse startLlm(@AuthenticationPrincipal AuthPrincipal principal) {
        return chatSessionService.startLlmSession(principal.userId());
    }

    @PostMapping("/lawyer")
    public ChatSessionResponse startLawyer(@AuthenticationPrincipal AuthPrincipal principal,
                                            @Valid @RequestBody StartLawyerSessionRequest request) {
        return chatSessionService.startLawyerSession(principal.userId(), request.lawyerId());
    }

    @PostMapping("/{sessionId}/end")
    public void end(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long sessionId) {
        chatSessionService.getOwnedEntity(principal.userId(), null, sessionId);
        chatSessionService.endSession(sessionId, "customer ended the session");
    }

    @GetMapping("/{sessionId}/messages")
    public List<ChatMessageResponse> history(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long sessionId) {
        chatSessionService.getOwnedEntity(principal.userId(), null, sessionId);
        return chatMessageService.history(sessionId);
    }
}
