package com.barboya.legal;

import jakarta.persistence.Entity;
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
@Table(name = "legal_acceptances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalAcceptance {

    @Id
    private UUID id;
    private UUID userId;
    private String document;
    private String version;
    private Instant acceptedAt;
    private String ip;
    private String userAgent;

    LegalAcceptance(UUID userId, String document, String version, Instant acceptedAt, String ip, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.document = document;
        this.version = version;
        this.acceptedAt = acceptedAt;
        this.ip = ip;
        this.userAgent = userAgent;
    }
}
