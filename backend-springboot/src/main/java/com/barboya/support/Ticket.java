package com.barboya.support;

import com.barboya.common.BusinessDays;
import com.barboya.common.Colombia;
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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    public enum Category {
        ORDER_PROBLEM,
        DELIVERY_FAILED,
        PAYMENT,
        ACCOUNT,
        OTHER,
        PERSONAL_DATA;

        /** Dias habiles para responder: las consultas de datos personales tienen el plazo mas corto. */
        int businessDays() {
            return this == PERSONAL_DATA ? 10 : 15;
        }
    }

    public enum Status {
        OPEN,
        ANSWERED,
        RESOLVED
    }

    @Id
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Long number;

    private UUID openerId;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String subject;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String resolution;
    private UUID resolvedBy;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;
    /** Fecha limite legal de respuesta. */
    private Instant dueAt;

    @Version
    private Long version;

    Ticket(UUID openerId, UUID orderId, Category category, String subject, Instant now) {
        this.id = UUID.randomUUID();
        this.openerId = openerId;
        this.orderId = orderId;
        this.category = category;
        this.subject = subject;
        this.status = Status.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
        this.dueAt = deadlineFrom(now);
    }

    private Instant deadlineFrom(Instant now) {
        return BusinessDays.deadline(LocalDate.ofInstant(now, Colombia.ZONE), category.businessDays());
    }

    /** Un mensaje nuevo reabre un caso resuelto. */
    void replied(boolean byStaff, Instant now) {
        // Reabrir un caso inicia un plazo nuevo
        if (status == Status.RESOLVED) {
            dueAt = deadlineFrom(now);
        }
        status = byStaff ? Status.ANSWERED : Status.OPEN;
        resolution = null;
        resolvedBy = null;
        updatedAt = now;
    }

    void resolve(String text, UUID staffId, Instant now) {
        status = Status.RESOLVED;
        resolution = text;
        resolvedBy = staffId;
        updatedAt = now;
    }
}
