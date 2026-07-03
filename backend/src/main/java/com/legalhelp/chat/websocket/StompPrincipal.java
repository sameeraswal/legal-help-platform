package com.legalhelp.chat.websocket;

import com.legalhelp.common.security.AuthPrincipal;

import java.security.Principal;

public record StompPrincipal(AuthPrincipal authPrincipal) implements Principal {
    @Override
    public String getName() {
        return String.valueOf(authPrincipal.userId());
    }
}
