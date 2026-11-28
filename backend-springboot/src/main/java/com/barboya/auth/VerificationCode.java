package com.barboya.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCode {

    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }

    @Id
    private UUID id;
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    private String codeHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant consumedAt;
    private int attempts;

    VerificationCode(UUID userId, Purpose purpose, String codeHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isUsable(Instant now, int maxAttempts) {
        return consumedAt == null && expiresAt.isAfter(now) && attempts < maxAttempts;
    }

    void registerFailure() {
        attempts++;
    }

    void consume(Instant now) {
        consumedAt = now;
    }
}
