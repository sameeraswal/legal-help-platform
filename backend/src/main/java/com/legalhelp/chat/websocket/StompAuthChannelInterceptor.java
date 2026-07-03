package com.legalhelp.chat.websocket;

import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.common.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * The HTTP-layer JWT filter (common.security.JwtAuthenticationFilter) does not run
 * for the SockJS handshake in a way that carries through to individual STOMP frames,
 * so authentication happens here instead: every CONNECT frame must carry an
 * Authorization header, validated the same way as REST requests.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new StompAuthenticationException("Missing Authorization header on CONNECT");
            }
            try {
                AuthPrincipal principal = jwtService.parse(authHeader.substring(7));
                accessor.setUser(new StompPrincipal(principal));
            } catch (JwtService.InvalidTokenException e) {
                throw new StompAuthenticationException("Invalid or expired token");
            }
        }
        return message;
    }

    public static class StompAuthenticationException extends RuntimeException {
        public StompAuthenticationException(String message) {
            super(message);
        }
    }
}
