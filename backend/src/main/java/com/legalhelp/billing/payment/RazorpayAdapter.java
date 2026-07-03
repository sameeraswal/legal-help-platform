package com.legalhelp.billing.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.legalhelp.admin.service.AppConfigService;
import com.legalhelp.common.exception.ApiException;
import com.legalhelp.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Razorpay REST integration over plain HTTP (no third-party SDK dependency, so the
 * exact request/response contract stays fully under our control). Order creation,
 * refunds, and HMAC-SHA256 webhook signature verification per Razorpay's documented API.
 */
@Component
public class RazorpayAdapter implements PaymentGatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(RazorpayAdapter.class);
    private static final String BASE_URL = "https://api.razorpay.com/v1";

    private final AppConfigService appConfigService;
    private final RestClient restClient = RestClient.create();

    public RazorpayAdapter(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @Override
    public PgOrder createOrder(long amountMinorUnits, String receiptId) {
        try {
            OrderResponse response = restClient.post()
                    .uri(BASE_URL + "/orders")
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .body(Map.of("amount", amountMinorUnits, "currency", "INR", "receipt", receiptId))
                    .retrieve()
                    .body(OrderResponse.class);
            if (response == null) {
                throw new ApiException(ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_GATEWAY, "Empty response from payment gateway");
            }
            return new PgOrder(response.id(), response.amount(), response.currency());
        } catch (Exception e) {
            log.error("Razorpay order creation failed for receipt={}", receiptId, e);
            throw new ApiException(ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_GATEWAY, "Failed to create payment order");
        }
    }

    @Override
    public PgRefundResult refund(String pgPaymentRef, long amountMinorUnits) {
        try {
            RefundResponse response = restClient.post()
                    .uri(BASE_URL + "/payments/" + pgPaymentRef + "/refund")
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .body(Map.of("amount", amountMinorUnits))
                    .retrieve()
                    .body(RefundResponse.class);
            if (response == null) {
                throw new ApiException(ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_GATEWAY, "Empty response from payment gateway");
            }
            return new PgRefundResult(response.id(), response.status());
        } catch (Exception e) {
            log.error("Razorpay refund failed for payment={}", pgPaymentRef, e);
            throw new ApiException(ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_GATEWAY, "Failed to process refund");
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appConfigService.getPgWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    private String basicAuthHeader() {
        String credentials = appConfigService.getPgKeyId() + ":" + appConfigService.getPgKeySecret();
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    static String newReceiptId() {
        return UUID.randomUUID().toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OrderResponse(String id, long amount, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RefundResponse(String id, String status) {
    }
}
