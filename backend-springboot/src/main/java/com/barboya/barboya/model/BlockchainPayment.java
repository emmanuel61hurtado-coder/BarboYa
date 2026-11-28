package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blockchain_payments")
@Getter
@Setter
public class BlockchainPayment {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String txHash;
    private String payerAddress;
    private long tokenAmountUnits;
    private int amountCop;
    private int confirmations = 0;

    @Enumerated(EnumType.STRING)
    private Enums.BlockchainTxStatus status = Enums.BlockchainTxStatus.pending;

    private String failureReason;
    private Instant createdAt = Instant.now();
    private Instant confirmedAt;
}