package com.legalhelp.billing.service;

import com.legalhelp.billing.dto.LawyerRateResponse;
import com.legalhelp.billing.entity.LawyerRate;
import com.legalhelp.billing.repository.LawyerRateRepository;
import com.legalhelp.common.audit.AuditLogService;
import com.legalhelp.common.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** New rates are appended, never edited in place — {@code effective_from} makes rate history auditable. */
@Service
public class LawyerRateService {

    private final LawyerRateRepository repository;
    private final AuditLogService auditLogService;

    public LawyerRateService(LawyerRateRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LawyerRateResponse setRate(Long lawyerId, long perMinuteRateMinorUnits, AuthPrincipal actor) {
        LawyerRate rate = new LawyerRate(lawyerId, perMinuteRateMinorUnits);
        rate = repository.save(rate);
        String entity = lawyerId == null ? "lawyer_rate:global" : "lawyer_rate:" + lawyerId;
        auditLogService.record(actor.userId(), actor.role(), "SET_RATE", entity, String.valueOf(rate.getId()), null, toResponse(rate));
        return toResponse(rate);
    }

    @Transactional(readOnly = true)
    public List<LawyerRateResponse> history(Long lawyerId) {
        return repository.findByLawyerIdOrderByEffectiveFromDesc(lawyerId).stream().map(this::toResponse).toList();
    }

    private LawyerRateResponse toResponse(LawyerRate rate) {
        return new LawyerRateResponse(rate.getId(), rate.getLawyerId(), rate.getPerMinuteRateMinorUnits(), rate.getEffectiveFrom());
    }
}
