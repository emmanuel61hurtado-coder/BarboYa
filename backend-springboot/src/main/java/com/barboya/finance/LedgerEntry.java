package com.barboya.finance;

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
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry {

    public enum Kind {
        COMMISSION,
        PAYMENT,
        ADJUSTMENT,
        REVERSAL
    }

    /** Como recibio BarboYa un pago registrado a mano. */
    public enum Method {
        BANK_TRANSFER,
        NEQUI,
        CASH
    }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    private UUID partyId;
    private UUID orderId;
    private int amountCop;

    @Enumerated(EnumType.STRING)
    private Method method;

    private String reference;
    private String note;
    private UUID reversalOf;
    private UUID createdBy;
    private Instant createdAt;

    LedgerEntry(Kind kind, UUID partyId, UUID orderId, int amountCop, Method method, String reference,
                String note, UUID reversalOf, UUID createdBy, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.kind = kind;
        this.partyId = partyId;
        this.orderId = orderId;
        this.amountCop = amountCop;
        this.method = method;
        this.reference = reference;
        this.note = note;
        this.reversalOf = reversalOf;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
