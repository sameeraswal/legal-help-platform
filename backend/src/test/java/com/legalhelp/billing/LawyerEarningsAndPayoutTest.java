package com.legalhelp.billing;

import com.legalhelp.auth.entity.User;
import com.legalhelp.auth.repository.UserRepository;
import com.legalhelp.billing.dto.PayoutDecisionRequest;
import com.legalhelp.billing.entity.LawyerWallet;
import com.legalhelp.billing.service.LawyerRateService;
import com.legalhelp.billing.service.LawyerWalletService;
import com.legalhelp.billing.service.PayoutService;
import com.legalhelp.common.exception.ApiException;
import com.legalhelp.common.security.AuthPrincipal;
import com.legalhelp.common.security.Role;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CLAUDE.md §Testing Priorities #4: lawyer earnings calculation and payout threshold checks.
 */
class LawyerEarningsAndPayoutTest extends IntegrationTestBase {

    @Autowired
    private LawyerRateService lawyerRateService;

    @Autowired
    private LawyerWalletService lawyerWalletService;

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.billing.default-payout-threshold-minor-units}")
    private long defaultThreshold;

    private AuthPrincipal admin;

    @BeforeEach
    void createAdmin() {
        User adminUser = userRepository.findByEmail("lawyer-earnings-admin@example.com").orElseGet(() -> userRepository.save(
                new User(Role.ADMIN, "Admin", "lawyer-earnings-admin@example.com", null, passwordEncoder.encode("Password123!"))));
        admin = new AuthPrincipal(adminUser.getId(), adminUser.getEmail(), "ADMIN");
    }

    private Long createLawyer(String email) {
        User lawyer = new User(Role.LAWYER, "Lawyer", email, null, passwordEncoder.encode("Password123!"));
        return userRepository.save(lawyer).getId();
    }

    @Test
    void creditEarning_computesAmountFromPerMinuteRate() {
        Long lawyerId = createLawyer("lawyer-earnings-1@example.com");
        // 500 paise/minute global rate; 90 seconds of billed chat = 1.5 minutes = 750 paise.
        lawyerRateService.setRate(null, 500L, admin);

        lawyerWalletService.creditEarning(lawyerId, 90, "test-session-1");

        LawyerWallet wallet = lawyerWalletService.getOrCreateWallet(lawyerId);
        assertThat(wallet.getBalanceMinorUnits()).isEqualTo(750L);
    }

    @Test
    void perLawyerRateOverride_takesPrecedenceOverGlobal() {
        Long lawyerId = createLawyer("lawyer-earnings-2@example.com");
        lawyerRateService.setRate(null, 500L, admin);
        lawyerRateService.setRate(lawyerId, 1000L, admin);

        lawyerWalletService.creditEarning(lawyerId, 60, "test-session-2");

        assertThat(lawyerWalletService.getOrCreateWallet(lawyerId).getBalanceMinorUnits()).isEqualTo(1000L);
    }

    @Test
    void payoutRequest_rejectedBelowThreshold_allowedAboveIt() {
        Long lawyerId = createLawyer("lawyer-earnings-3@example.com");
        lawyerRateService.setRate(null, 500L, admin);

        // Earn less than the configured payout threshold.
        lawyerWalletService.creditEarning(lawyerId, 60, "below-threshold");
        assertThat(lawyerWalletService.getOrCreateWallet(lawyerId).getBalanceMinorUnits()).isLessThan(defaultThreshold);

        assertThatThrownBy(() -> payoutService.requestPayout(lawyerId)).isInstanceOf(ApiException.class);

        // Earn enough to clear the threshold.
        long shortfall = defaultThreshold - lawyerWalletService.getOrCreateWallet(lawyerId).getBalanceMinorUnits();
        long secondsNeeded = (shortfall / 500L + 1) * 60;
        lawyerWalletService.creditEarning(lawyerId, secondsNeeded, "above-threshold");

        var request = payoutService.requestPayout(lawyerId);
        assertThat(request.status().name()).isEqualTo("PENDING");

        long balanceBeforePayout = lawyerWalletService.getOrCreateWallet(lawyerId).getBalanceMinorUnits();
        var decided = payoutService.decide(request.id(), new PayoutDecisionRequest(true, "NEFT-REF-123"), admin);
        assertThat(decided.status().name()).isEqualTo("PAID");
        assertThat(lawyerWalletService.getOrCreateWallet(lawyerId).getBalanceMinorUnits())
                .isEqualTo(balanceBeforePayout - request.amountMinorUnits());
    }
}
