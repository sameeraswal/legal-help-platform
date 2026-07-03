package com.legalhelp.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalhelp.billing.entity.Payment;
import com.legalhelp.billing.entity.PaymentStatus;
import com.legalhelp.billing.entity.Plan;
import com.legalhelp.billing.payment.PaymentGatewayAdapter;
import com.legalhelp.billing.repository.PaymentRepository;
import com.legalhelp.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Idempotent-by-construction (CLAUDE.md domain rule #3): every webhook is signature-verified,
 * the affected Payment row is pessimistically locked before mutation, and a payment already in
 * a terminal state is a silent no-op — replays and out-of-order deliveries are always safe.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PaymentGatewayAdapter paymentGatewayAdapter;
    private final PaymentRepository paymentRepository;
    private final PlanService planService;
    private final WalletService walletService;

    public WebhookService(PaymentGatewayAdapter paymentGatewayAdapter, PaymentRepository paymentRepository,
                           PlanService planService, WalletService walletService) {
        this.paymentGatewayAdapter = paymentGatewayAdapter;
        this.paymentRepository = paymentRepository;
        this.planService = planService;
        this.walletService = walletService;
    }

    @Transactional
    public void handle(String rawBody, String signatureHeader) {
        if (!paymentGatewayAdapter.verifyWebhookSignature(rawBody, signatureHeader)) {
            throw new BadRequestException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(rawBody);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }

        String event = root.path("event").asText("");
        JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String pgOrderId = paymentEntity.path("order_id").asText(null);
        String pgPaymentId = paymentEntity.path("id").asText(null);

        if (pgOrderId == null) {
            log.warn("Webhook event {} carried no order_id; ignoring", event);
            return;
        }

        Optional<Payment> maybePayment = paymentRepository.findByPgOrderIdForUpdate(pgOrderId);
        if (maybePayment.isEmpty()) {
            log.warn("Webhook referenced unknown pg_order_id={}", pgOrderId);
            return;
        }
        Payment payment = maybePayment.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment {} already in terminal status {}, ignoring replayed webhook", payment.getId(), payment.getStatus());
            return;
        }

        payment.setRawWebhookPayload(rawBody);

        switch (event) {
            case "payment.captured" -> {
                if (pgPaymentId != null && paymentRepository.existsByPgRef(pgPaymentId)) {
                    log.warn("pg_ref {} already recorded on another payment; ignoring duplicate delivery", pgPaymentId);
                    return;
                }
                payment.setPgRef(pgPaymentId);
                payment.setStatus(PaymentStatus.SUCCESS);
                Plan plan = planService.getRequired(payment.getPlanId());
                walletService.rechargeSeconds(payment.getCustomerId(), plan.getSeconds(), "payment:" + payment.getId());
            }
            case "payment.failed" -> payment.setStatus(PaymentStatus.FAILED);
            default -> log.info("Ignoring unhandled webhook event type: {}", event);
        }
    }
}
