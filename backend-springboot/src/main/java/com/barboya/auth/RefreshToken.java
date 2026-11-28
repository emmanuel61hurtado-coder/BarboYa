package com.barboya.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private UUID id;
    private UUID sessionId;
    private String tokenHash;
    private Instant createdAt;
    private Instant usedAt;
    private UUID parentId;
    private Instant discardedAt;

    RefreshToken(UUID sessionId, UUID parentId, String tokenHash, Instant now) {
        this.id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.parentId = parentId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
    }

    void markUsed(Instant now) {
        usedAt = now;
    }

    void discard(Instant now) {
        discardedAt = now;
    }
}
