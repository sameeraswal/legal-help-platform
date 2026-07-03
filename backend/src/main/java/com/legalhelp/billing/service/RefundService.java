package com.legalhelp.billing.service;

import com.legalhelp.billing.dto.RefundResponse;
import com.legalhelp.billing.entity.Payment;
import com.legalhelp.billing.entity.PaymentStatus;
import com.legalhelp.billing.entity.Plan;
import com.legalhelp.billing.entity.Refund;
import com.legalhelp.billing.entity.RefundStatus;
import com.legalhelp.billing.payment.PaymentGatewayAdapter;
import com.legalhelp.billing.payment.PgRefundResult;
import com.legalhelp.billing.repository.PaymentRepository;
import com.legalhelp.billing.repository.RefundRepository;
import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refunds only ever go through the PG refund API plus a compensating ledger entry
 * (CLAUDE.md domain rule #7) — the original Payment row's amount is never mutated,
 * only its status.
 */
@Service
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PlanService planService;
    private final PaymentGatewayAdapter paymentGatewayAdapter;
    private final WalletService walletService;
    private final AuditLogService auditLogService;

    public RefundService(PaymentRepository paymentRepository, RefundRepository refundRepository,
                          PlanService planService, PaymentGatewayAdapter paymentGatewayAdapter,
                          WalletService walletService, AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.planService = planService;
        this.paymentGatewayAdapter = paymentGatewayAdapter;
        this.walletService = walletService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RefundResponse initiateRefund(Long paymentId, AuthPrincipal actor) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCESS || payment.getPgRef() == null) {
            throw new BadRequestException("Only successful, captured payments can be refunded");
        }

        PgRefundResult pgResult = paymentGatewayAdapter.refund(payment.getPgRef(), payment.getAmountMinorUnits());

        Refund refund = new Refund(payment.getId(), payment.getAmountMinorUnits(), actor.userId());
        refund.setPgRefundRef(pgResult.pgRefundId());
        refund.setStatus("processed".equalsIgnoreCase(pgResult.status()) ? RefundStatus.SUCCESS : RefundStatus.INITIATED);
        refund = refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);

        Plan plan = planService.getRequired(payment.getPlanId());
        walletService.adjustSecondsWithRefundEntry(payment.getCustomerId(), -plan.getSeconds(), "refund:" + refund.getId());

        auditLogService.record(actor.userId(), actor.role(), "REFUND", "payment", String.valueOf(payment.getId()),
                null, toResponse(refund));

        return toResponse(refund);
    }

    private RefundResponse toResponse(Refund refund) {
        return new RefundResponse(refund.getId(), refund.getPaymentId(), refund.getAmountMinorUnits(),
                refund.getStatus(), refund.getCreatedAt());
    }
}
