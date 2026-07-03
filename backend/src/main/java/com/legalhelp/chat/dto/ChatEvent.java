package com.legalhelp.chat.dto;

/** Envelope pushed to the client over STOMP at /topic/sessions/{sessionId}. */
public record ChatEvent(OutgoingEventType type, Object payload) {
    public static ChatEvent of(OutgoingEventType type, Object payload) {
        return new ChatEvent(type, payload);
    }
}
