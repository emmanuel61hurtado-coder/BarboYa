package com.barboya.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

    @Id
    private UUID id;
    private UUID actorId;
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String detail;
    private String ip;
    private String userAgent;
    private Instant createdAt;

    AuditEvent(UUID actorId, UUID targetId, AuditAction action, String detail, String ip, String userAgent,
               Instant createdAt) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.targetId = targetId;
        this.action = action;
        this.detail = detail;
        this.ip = ip;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }
}
