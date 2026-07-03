package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.CustomerWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerWalletRepository extends JpaRepository<CustomerWallet, Long> {

    /** Row lock held for the duration of the enclosing transaction — see WalletService. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from CustomerWallet w where w.customerId = :customerId")
    Optional<CustomerWallet> findByIdForUpdate(Long customerId);
}
