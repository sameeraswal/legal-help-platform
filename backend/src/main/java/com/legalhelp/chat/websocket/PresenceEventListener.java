package com.legalhelp.chat.websocket;

import com.legalhelp.chat.service.PresenceService;
import com.legalhelp.common.security.Role;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** A lawyer's WebSocket connection dropping (tab closed, network loss) always marks them offline. */
@Component
public class PresenceEventListener {

    private final PresenceService presenceService;

    public PresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() instanceof StompPrincipal stompPrincipal
                && stompPrincipal.authPrincipal().role().equals(Role.LAWYER.name())) {
            presenceService.setOffline(stompPrincipal.authPrincipal().userId());
        }
    }
}
