package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @Enumerated(EnumType.STRING)
    private Enums.SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    private Enums.SubscriptionStatus status = Enums.SubscriptionStatus.pending;

    private int amountCop;
    private Instant startsAt;
    private Instant expiresAt;
    private Instant createdAt = Instant.now();
}