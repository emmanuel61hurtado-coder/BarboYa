package com.barboya.couriers;

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
import java.time.LocalDate;
import java.util.UUID;

/** Reemplazar un documento crea otro registro: una revision nunca cambia de archivo. */
@Entity
@Table(name = "courier_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierDocument {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    private UUID id;
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String storageKey;
    private String contentType;
    private int sizeBytes;
    private LocalDate expiresOn;
    private String reviewNote;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Instant uploadedAt;

    @Version
    private Long version;

    CourierDocument(UUID userId, DocumentType type, String storageKey, String contentType, int sizeBytes,
                    LocalDate expiresOn, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.status = Status.PENDING;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.expiresOn = expiresOn;
        this.uploadedAt = now;
    }

    boolean isExpired(LocalDate today) {
        return expiresOn != null && expiresOn.isBefore(today);
    }

    void review(Status decision, String note, UUID reviewer, Instant now) {
        status = decision;
        reviewNote = note;
        reviewedBy = reviewer;
        reviewedAt = now;
    }
}
