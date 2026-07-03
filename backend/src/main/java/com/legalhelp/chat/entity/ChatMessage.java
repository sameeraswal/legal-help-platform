package com.legalhelp.chat.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Insert-only (CLAUDE.md domain rule #5) — no update/delete methods exist for this entity. */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageSender sender;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    protected ChatMessage() {
    }

    public ChatMessage(Long sessionId, MessageSender sender, String content) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public MessageSender getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
