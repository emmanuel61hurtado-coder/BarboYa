package com.barboya.ratings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ratings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rating {

    public enum Target {
        BUSINESS,
        COURIER
    }

    @Id
    private UUID id;

    @Column(updatable = false)
    private UUID orderId;
    @Column(updatable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private Target targetType;
    @Column(updatable = false)
    private UUID targetId;
    @Column(updatable = false)
    private int stars;
    @Column(updatable = false)
    private String comment;

    private boolean hidden;
    private String hiddenReason;

    @Column(updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    Rating(UUID orderId, UUID authorId, Target targetType, UUID targetId, int stars, String comment, Instant now) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.authorId = authorId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.stars = stars;
        this.comment = comment;
        this.createdAt = now;
    }

    void hide(String reason) {
        hidden = true;
        hiddenReason = reason;
    }

    void restore() {
        hidden = false;
        hiddenReason = null;
    }
}
