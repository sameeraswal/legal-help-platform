package com.legalhelp.chat.controller;

import com.legalhelp.chat.dto.OnlineLawyerResponse;
import com.legalhelp.chat.service.PresenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lawyers")
public class LawyerDirectoryController {

    private final PresenceService presenceService;

    public LawyerDirectoryController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/online")
    public List<OnlineLawyerResponse> online() {
        return presenceService.listOnline();
    }
}
