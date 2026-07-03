package com.legalhelp.admin.controller;

import com.legalhelp.admin.dto.AppConfigResponse;
import com.legalhelp.admin.dto.GeneralConfigUpdateRequest;
import com.legalhelp.admin.dto.PgConfigUpdateRequest;
import com.legalhelp.admin.entity.AppConfig;
import com.legalhelp.admin.service.AppConfigService;
import com.legalhelp.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/config")
public class AppConfigAdminController {

    private final AppConfigService appConfigService;

    public AppConfigAdminController(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @GetMapping
    public AppConfigResponse get() {
        AppConfig config = appConfigService.getOrCreate();
        return new AppConfigResponse(config.getFreeMinutes(), config.getPayoutThresholdMinorUnits(),
                appConfigService.isPgConfiguredExplicitly());
    }

    @PutMapping("/general")
    public AppConfigResponse updateGeneral(@Valid @RequestBody GeneralConfigUpdateRequest request,
                                            @AuthenticationPrincipal AuthPrincipal actor) {
        appConfigService.updateGeneralConfig(request.freeMinutes(), request.payoutThresholdMinorUnits(), actor);
        return get();
    }

    @PutMapping("/payment-gateway")
    public AppConfigResponse updatePaymentGateway(@Valid @RequestBody PgConfigUpdateRequest request,
                                                   @AuthenticationPrincipal AuthPrincipal actor) {
        appConfigService.updatePgConfig(request.keyId(), request.keySecret(), request.webhookSecret(), actor);
        return get();
    }
}
