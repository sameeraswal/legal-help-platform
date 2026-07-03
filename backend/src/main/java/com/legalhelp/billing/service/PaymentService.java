package com.legalhelp.billing.service;

import com.legalhelp.admin.service.AppConfigService;
import com.legalhelp.billing.dto.PaymentResponse;
import com.legalhelp.billing.dto.PurchaseInitiateResponse;
import com.legalhelp.billing.entity.Payment;
import com.legalhelp.billing.entity.PaymentStatus;
import com.legalhelp.billing.entity.Plan;
import com.legalhelp.billing.payment.PaymentGatewayAdapter;
import com.legalhelp.billing.payment.PgOrder;
import com.legalhelp.billing.repository.PaymentRepository;
import com.legalhelp.common.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PlanService planService;
    private final PaymentGatewayAdapter paymentGatewayAdapter;
    private final AppConfigService appConfigService;

    public PaymentService(PaymentRepository paymentRepository, PlanService planService,
                           PaymentGatewayAdapter paymentGatewayAdapter, AppConfigService appConfigService) {
        this.paymentRepository = paymentRepository;
        this.planService = planService;
        this.paymentGatewayAdapter = paymentGatewayAdapter;
        this.appConfigService = appConfigService;
    }

    @Transactional
    public PurchaseInitiateResponse initiatePurchase(Long customerId, Long planId) {
        Plan plan = planService.getRequired(planId);
        if (!plan.isActive()) {
            throw new BadRequestException("This plan is no longer available");
        }
        String orderId = UUID.randomUUID().toString();
        Payment payment = new Payment(orderId, customerId, planId, plan.getPriceMinorUnits());

        PgOrder pgOrder = paymentGatewayAdapter.createOrder(plan.getPriceMinorUnits(), orderId);
        payment.setPgOrderId(pgOrder.pgOrderId());
        paymentRepository.save(payment);

        return new PurchaseInitiateResponse(orderId, pgOrder.pgOrderId(), plan.getPriceMinorUnits(), pgOrder.currency(),
                appConfigService.getPgKeyId());
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listByStatus(PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listAll(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getCustomerId(), payment.getPlanId(),
                payment.getAmountMinorUnits(), payment.getStatus(), payment.getCreatedAt());
    }
}
