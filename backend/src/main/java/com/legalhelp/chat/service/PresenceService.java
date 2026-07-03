package com.legalhelp.chat.service;

import com.legalhelp.auth.service.UserDirectoryService;
import com.legalhelp.chat.dto.ChatEvent;
import com.legalhelp.chat.dto.OnlineLawyerResponse;
import com.legalhelp.chat.dto.OutgoingEventType;
import com.legalhelp.common.exception.BadRequestException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory presence — acceptable for a single-instance deployment. A multi-instance
 * deployment would move this to a shared store (e.g. Redis) keyed the same way.
 */
@Service
public class PresenceService {

    private static final String TOPIC = "/topic/lawyers/online";

    private final Set<Long> onlineLawyerIds = ConcurrentHashMap.newKeySet();
    private final UserDirectoryService userDirectoryService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(UserDirectoryService userDirectoryService, SimpMessagingTemplate messagingTemplate) {
        this.userDirectoryService = userDirectoryService;
        this.messagingTemplate = messagingTemplate;
    }

    public void setOnline(Long lawyerId) {
        if (!userDirectoryService.isApprovedLawyer(lawyerId)) {
            throw new BadRequestException("Only approved lawyers can go online");
        }
        if (onlineLawyerIds.add(lawyerId)) {
            broadcast();
        }
    }

    public void setOffline(Long lawyerId) {
        if (onlineLawyerIds.remove(lawyerId)) {
            broadcast();
        }
    }

    public List<OnlineLawyerResponse> listOnline() {
        if (onlineLawyerIds.isEmpty()) {
            return List.of();
        }
        var profiles = userDirectoryService.getProfiles(List.copyOf(onlineLawyerIds));
        return onlineLawyerIds.stream()
                .map(profiles::get)
                .filter(java.util.Objects::nonNull)
                .map(p -> new OnlineLawyerResponse(p.id(), p.name()))
                .toList();
    }

    private void broadcast() {
        messagingTemplate.convertAndSend(TOPIC, ChatEvent.of(OutgoingEventType.PRESENCE_UPDATE, listOnline()));
    }
}
