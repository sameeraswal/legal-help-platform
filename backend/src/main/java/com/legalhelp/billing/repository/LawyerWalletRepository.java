package com.legalhelp.billing.repository;

import com.legalhelp.billing.entity.LawyerWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LawyerWalletRepository extends JpaRepository<LawyerWallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from LawyerWallet w where w.lawyerId = :lawyerId")
    Optional<LawyerWallet> findByIdForUpdate(Long lawyerId);
}
