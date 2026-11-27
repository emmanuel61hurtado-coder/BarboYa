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

import java.util.UUID;

@Entity
@Immutable
@Table(name = "ledger_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerLine {

    public enum Account {
        /** Lo que un comercio o domiciliario debe a BarboYa. */
        RECEIVABLE,
        COMMISSION_REVENUE,
        PLATFORM_FUNDS,
        /** Comisiones perdonadas. */
        WAIVED
    }

    @Id
    private UUID id;
    private UUID entryId;

    @Enumerated(EnumType.STRING)
    private Account account;

    private UUID partyId;
    private int debitCop;
    private int creditCop;

    LedgerLine(UUID entryId, Account account, UUID partyId, int debitCop, int creditCop) {
        this.id = UUID.randomUUID();
        this.entryId = entryId;
        this.account = account;
        this.partyId = account == Account.RECEIVABLE ? partyId : null;
        this.debitCop = debitCop;
        this.creditCop = creditCop;
    }
}
