package com.legalhelp.billing;

import com.legalhelp.auth.entity.User;
import com.legalhelp.auth.repository.UserRepository;
import com.legalhelp.billing.entity.CustomerWallet;
import com.legalhelp.billing.entity.WalletLedger;
import com.legalhelp.billing.repository.WalletLedgerRepository;
import com.legalhelp.billing.service.WalletService;
import com.legalhelp.common.security.Role;
import com.legalhelp.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent consume vs. recharge on the same customer wallet must never lose an
 * update — the row-level PESSIMISTIC_WRITE lock in WalletService is what this
 * test exercises (CLAUDE.md §Testing Priorities #1).
 */
class WalletConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletLedgerRepository ledgerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long customerId;

    @BeforeEach
    void createCustomer() {
        User user = new User(Role.CUSTOMER, "Concurrency Test Customer", "concurrency-test@example.com",
                null, passwordEncoder.encode("Password123!"));
        customerId = userRepository.save(user).getId();
    }

    @Test
    void concurrentConsumeAndRecharge_neverLosesAnUpdate() throws InterruptedException {
        // Drain the account's free-minutes allowance first so the concurrent consumes below
        // are forced to draw from the paid balance this test actually exercises.
        long freeSeconds = walletService.availableSeconds(customerId);
        walletService.consumeSeconds(customerId, freeSeconds, "drain-free-for-test");
        walletService.rechargeSeconds(customerId, 10_000, "seed");

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicLong totalRecharged = new AtomicLong(10_000);
        AtomicLong totalConsumed = new AtomicLong(0);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            boolean recharge = i % 2 == 0;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    if (recharge) {
                        walletService.rechargeSeconds(customerId, 100, "concurrent-recharge");
                        totalRecharged.addAndGet(100);
                    } else {
                        walletService.consumeSeconds(customerId, 50, "concurrent-consume");
                        totalConsumed.addAndGet(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(errors).isEmpty();

        CustomerWallet wallet = walletService.getOrCreateWallet(customerId);
        long expectedRemaining = totalRecharged.get() - totalConsumed.get();
        assertThat(wallet.getRemainingSeconds()).isEqualTo(expectedRemaining);

        // The ledger is the source of truth — the cached wallet balance must always
        // equal the running sum of ledger deltas for this customer.
        List<WalletLedger> allEntries = ledgerRepository
                .findByUserIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, 100, Sort.by("id")))
                .getContent();
        long sumFromLedger = allEntries.stream().mapToLong(WalletLedger::getSecondsDelta).sum();
        assertThat(sumFromLedger).isEqualTo(wallet.totalAvailableSeconds());
    }
}
