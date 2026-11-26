package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

    @Enumerated(EnumType.STRING)
    private Enums.ServiceType serviceType = Enums.ServiceType.passenger;

    private double originLat;
    private double originLng;
    private String originAddress;
    private double destLat;
    private double destLng;
    private String destAddress;

    private double distanceKm;
    private int suggestedFareCop;
    private int agreedFareCop;
    private int pin;

    @Enumerated(EnumType.STRING)
    private Enums.TripStatus status = Enums.TripStatus.requested;

    @Enumerated(EnumType.STRING)
    private Enums.PaymentMethod paymentMethod = Enums.PaymentMethod.cash;

    private String shareToken;
    private Instant createdAt = Instant.now();
    private Instant acceptedAt;
    private Instant completedAt;
    private Instant cancelledAt;
}