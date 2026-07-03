package com.legalhelp.billing.service;

import com.legalhelp.admin.service.AppConfigService;
import com.legalhelp.billing.dto.PayoutDecisionRequest;
import com.legalhelp.billing.dto.PayoutRequestResponse;
import com.legalhelp.billing.entity.LawyerWallet;
import com.legalhelp.billing.entity.PayoutRequest;
import com.legalhelp.billing.entity.PayoutStatus;
import com.legalhelp.billing.repository.PayoutRequestRepository;
import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.exception.BadRequestException;
import com.legalhelp.common.exception.ResourceNotFoundException;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approval directly triggers the wallet debit and records the (manually confirmed)
 * bank transfer reference — automated bank payouts via a PG payouts product are out
 * of scope; see docs/runbook.md.
 */
@Service
public class PayoutService {

    private final PayoutRequestRepository payoutRequestRepository;
    private final LawyerWalletService lawyerWalletService;
    private final AppConfigService appConfigService;
    private final AuditLogService auditLogService;

    public PayoutService(PayoutRequestRepository payoutRequestRepository, LawyerWalletService lawyerWalletService,
                          AppConfigService appConfigService, AuditLogService auditLogService) {
        this.payoutRequestRepository = payoutRequestRepository;
        this.lawyerWalletService = lawyerWalletService;
        this.appConfigService = appConfigService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PayoutRequestResponse requestPayout(Long lawyerId) {
        LawyerWallet wallet = lawyerWalletService.getOrCreateWallet(lawyerId);
        long threshold = appConfigService.getPayoutThresholdMinorUnits();
        if (wallet.getBalanceMinorUnits() < threshold) {
            throw new BadRequestException("Wallet balance is below the payout threshold");
        }
        PayoutRequest request = new PayoutRequest(lawyerId, wallet.getBalanceMinorUnits());
        return toResponse(payoutRequestRepository.save(request));
    }

    @Transactional
    public PayoutRequestResponse decide(Long payoutRequestId, PayoutDecisionRequest decision, AuthPrincipal actor) {
        PayoutRequest request = payoutRequestRepository.findById(payoutRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout request not found"));
        if (request.getStatus() != PayoutStatus.PENDING) {
            throw new BadRequestException("This payout request has already been decided");
        }
        request.setAdminId(actor.userId());

        if (decision.approve()) {
            lawyerWalletService.debitForPayout(request.getLawyerId(), request.getAmountMinorUnits(), "payout:" + request.getId());
            request.setBankReference(decision.bankReference());
            request.setStatus(PayoutStatus.PAID);
        } else {
            request.setStatus(PayoutStatus.REJECTED);
        }

        auditLogService.record(actor.userId(), actor.role(), decision.approve() ? "APPROVE_PAYOUT" : "REJECT_PAYOUT",
                "payout_request", String.valueOf(request.getId()), null, toResponse(request));

        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> listPending(Pageable pageable) {
        return payoutRequestRepository.findByStatus(PayoutStatus.PENDING, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PayoutRequestResponse> listForLawyer(Long lawyerId, Pageable pageable) {
        return payoutRequestRepository.findByLawyerIdOrderByCreatedAtDesc(lawyerId, pageable).map(this::toResponse);
    }

    private PayoutRequestResponse toResponse(PayoutRequest request) {
        return new PayoutRequestResponse(request.getId(), request.getLawyerId(), request.getAmountMinorUnits(),
                request.getStatus(), request.getBankReference(), request.getCreatedAt(), request.getDecidedAt());
    }
}
