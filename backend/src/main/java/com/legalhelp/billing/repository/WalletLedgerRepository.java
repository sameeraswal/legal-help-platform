package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.WalletLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insert-only — intentionally exposes no update/delete query methods. */
public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {
    Page<WalletLedger> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
