package com.legalhelp.billing.controller;

import com.legalhelp.billing.dto.LawyerRateResponse;
import com.legalhelp.billing.dto.LawyerRateSetRequest;
import com.legalhelp.billing.service.LawyerRateService;
import com.legalhelp.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/lawyer-rates")
public class LawyerRateAdminController {

    private final LawyerRateService lawyerRateService;

    public LawyerRateAdminController(LawyerRateService lawyerRateService) {
        this.lawyerRateService = lawyerRateService;
    }

    @PostMapping
    public LawyerRateResponse set(@Valid @RequestBody LawyerRateSetRequest request, @AuthenticationPrincipal AuthPrincipal actor) {
        return lawyerRateService.setRate(request.lawyerId(), request.perMinuteRateMinorUnits(), actor);
    }

    @GetMapping("/global")
    public List<LawyerRateResponse> globalHistory() {
        return lawyerRateService.history(null);
    }

    @GetMapping("/{lawyerId}")
    public List<LawyerRateResponse> lawyerHistory(@PathVariable Long lawyerId) {
        return lawyerRateService.history(lawyerId);
    }
}
