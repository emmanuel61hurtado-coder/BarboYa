package com.barboya.auth;

import com.barboya.auth.VerificationCode.Purpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, Purpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(UUID userId, Purpose purpose, Instant since);
}
