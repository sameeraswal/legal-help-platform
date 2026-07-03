package com.legalhelp.chat.dto;

public record WsIncomingMessage(Long sessionId, String content) {
}
