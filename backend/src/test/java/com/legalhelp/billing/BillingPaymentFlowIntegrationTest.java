package com.legalhelp.billing;

import com.legalhelp.auth.dto.AuthResponse;
import com.legalhelp.auth.dto.RegisterRequest;
import com.legalhelp.billing.dto.PlanResponse;
import com.legalhelp.billing.dto.PurchaseInitiateResponse;
import com.legalhelp.billing.dto.WalletBalanceResponse;
import com.legalhelp.billing.payment.PaymentGatewayAdapter;
import com.legalhelp.billing.payment.PgOrder;
import com.legalhelp.billing.payment.PgRefundResult;
import com.legalhelp.common.security.Role;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Covers CLAUDE.md §Testing Priorities #2: payment webhook idempotency and
 * signature verification — a replayed "payment.captured" delivery must never
 * credit the wallet twice.
 */
class BillingPaymentFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private PaymentGatewayAdapter paymentGatewayAdapter;

    @Test
    void webhookReplay_isIdempotent() {
        when(paymentGatewayAdapter.createOrder(anyLong(), anyString()))
                .thenReturn(new PgOrder("order_test_123", 1000, "INR"));
        when(paymentGatewayAdapter.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);

        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Billing Test", "billing-flow@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(registerResponse.getBody().accessToken());

        ResponseEntity<PlanResponse[]> plans = restTemplate.getForEntity("/api/plans", PlanResponse[].class);
        PlanResponse plan = plans.getBody()[0];

        ResponseEntity<PurchaseInitiateResponse> purchase = restTemplate.exchange(
                "/api/purchases/plans/" + plan.id(), HttpMethod.POST, new HttpEntity<>(headers), PurchaseInitiateResponse.class);
        assertThat(purchase.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(purchase.getBody().pgOrderId()).isEqualTo("order_test_123");

        String webhookPayload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test_abc",
                        "order_id": "order_test_123",
                        "amount": %d,
                        "status": "captured"
                      }
                    }
                  }
                }""".formatted(plan.priceMinorUnits());

        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
        webhookHeaders.set("X-Razorpay-Signature", "any-signature-since-verify-is-mocked");

        // First delivery credits the wallet.
        ResponseEntity<Void> first = restTemplate.postForEntity("/api/webhooks/razorpay",
                new HttpEntity<>(webhookPayload, webhookHeaders), Void.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<WalletBalanceResponse> balanceAfterFirst = restTemplate.exchange(
                "/api/wallet/balance", HttpMethod.GET, new HttpEntity<>(headers), WalletBalanceResponse.class);
        long paidSecondsAfterFirst = balanceAfterFirst.getBody().paidSecondsRemaining();
        assertThat(paidSecondsAfterFirst).isEqualTo(plan.seconds());

        // Replayed delivery (same payload, same signature) must be a no-op.
        ResponseEntity<Void> replay = restTemplate.postForEntity("/api/webhooks/razorpay",
                new HttpEntity<>(webhookPayload, webhookHeaders), Void.class);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<WalletBalanceResponse> balanceAfterReplay = restTemplate.exchange(
                "/api/wallet/balance", HttpMethod.GET, new HttpEntity<>(headers), WalletBalanceResponse.class);
        assertThat(balanceAfterReplay.getBody().paidSecondsRemaining()).isEqualTo(paidSecondsAfterFirst);
    }

    @Test
    void invalidSignature_isRejected() {
        when(paymentGatewayAdapter.verifyWebhookSignature(anyString(), anyString())).thenReturn(false);

        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
        webhookHeaders.set("X-Razorpay-Signature", "forged");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/webhooks/razorpay",
                new HttpEntity<>("{\"event\":\"payment.captured\"}", webhookHeaders), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refund_doesNotMutatePaymentAmount_andClawsBackSeconds() {
        when(paymentGatewayAdapter.createOrder(anyLong(), anyString()))
                .thenReturn(new PgOrder("order_refund_test", 2000, "INR"));
        when(paymentGatewayAdapter.verifyWebhookSignature(anyString(), anyString())).thenReturn(true);
        when(paymentGatewayAdapter.refund(anyString(), anyLong()))
                .thenReturn(new PgRefundResult("rfnd_test_1", "processed"));

        RegisterRequest register = new RegisterRequest(Role.CUSTOMER, "Refund Test", "refund-flow@example.com", null, "Password123!");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", register, AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(registerResponse.getBody().accessToken());

        ResponseEntity<PlanResponse[]> plans = restTemplate.getForEntity("/api/plans", PlanResponse[].class);
        PlanResponse plan = plans.getBody()[0];

        restTemplate.exchange("/api/purchases/plans/" + plan.id(), HttpMethod.POST, new HttpEntity<>(headers), PurchaseInitiateResponse.class);

        String webhookPayload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_refund_test","order_id":"order_refund_test","amount":%d,"status":"captured"}}}}
                """.formatted(plan.priceMinorUnits());
        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
        webhookHeaders.set("X-Razorpay-Signature", "mocked");
        restTemplate.postForEntity("/api/webhooks/razorpay", new HttpEntity<>(webhookPayload, webhookHeaders), Void.class);

        ResponseEntity<WalletBalanceResponse> beforeRefund = restTemplate.exchange(
                "/api/wallet/balance", HttpMethod.GET, new HttpEntity<>(headers), WalletBalanceResponse.class);
        assertThat(beforeRefund.getBody().paidSecondsRemaining()).isEqualTo(plan.seconds());

        // Refund is admin-only; a customer token must be rejected.
        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/api/admin/payments/1/refund", HttpMethod.POST, new HttpEntity<>(headers), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
