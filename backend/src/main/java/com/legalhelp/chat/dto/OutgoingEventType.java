package com.legalhelp.chat.dto;

public enum OutgoingEventType {
    MESSAGE,
    TOKEN_DELTA,
    TOKEN_COMPLETE,
    SESSION_WARNING,
    SESSION_ENDED,
    PRESENCE_UPDATE,
    ERROR
}
