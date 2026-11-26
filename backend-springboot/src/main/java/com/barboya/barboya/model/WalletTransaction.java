package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
public class WalletTransaction {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Enums.WalletTxType type;

    @Enumerated(EnumType.STRING)
    private Enums.WalletTxStatus status = Enums.WalletTxStatus.pending;

    private int amountCop;
    private String description;
    private Instant createdAt = Instant.now();
    private Instant decidedAt;
}