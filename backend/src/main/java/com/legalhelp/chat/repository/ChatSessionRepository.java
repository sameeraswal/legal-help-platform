package com.legalhelp.chat.repository;

import com.legalhelp.chat.entity.ChatSession;
import com.legalhelp.chat.entity.ChatSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByStatus(ChatSessionStatus status);

    Optional<ChatSession> findByCustomerIdAndStatus(Long customerId, ChatSessionStatus status);

    List<ChatSession> findByCustomerIdOrderByStartedAtDesc(Long customerId);

    List<ChatSession> findByLawyerIdOrderByStartedAtDesc(Long lawyerId);

    Optional<ChatSession> findByLawyerIdAndStatus(Long lawyerId, ChatSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChatSession s where s.id = :id")
    Optional<ChatSession> findByIdForUpdate(Long id);
}
