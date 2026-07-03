package com.legalhelp.billing.controller;

import com.legalhelp.billing.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoint (see SecurityConfig) — authenticity is established via HMAC signature, not a JWT. */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<Void> razorpay(@RequestBody String rawBody,
                                          @RequestHeader("X-Razorpay-Signature") String signature) {
        webhookService.handle(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
