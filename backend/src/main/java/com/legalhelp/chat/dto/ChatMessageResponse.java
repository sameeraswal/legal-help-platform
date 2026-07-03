package com.legalhelp.chat.dto;

import com.legalhelp.chat.entity.MessageSender;

import java.time.Instant;

public record ChatMessageResponse(Long id, Long sessionId, MessageSender sender, String content, Instant timestamp) {
}
