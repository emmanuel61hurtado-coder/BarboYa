package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
public class DriverProfile {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    private Enums.VehicleType vehicleType = Enums.VehicleType.moto;

    private String plate;
    private boolean online = false;
    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    private Enums.VerificationStatus verificationStatus = Enums.VerificationStatus.pending;

    private double walletBalanceCop = 0;
    private double subscriptionExpiresAtEpoch = 0;

    @Enumerated(EnumType.STRING)
    private Enums.BillingScheme billingScheme = Enums.BillingScheme.per_trip_fee;

    private int tripFeeOverrideCop = 0; // 0 = use zone/default

    private double lastLat;
    private double lastLng;
    private Instant lastSeenAt;

    private Instant createdAt = Instant.now();
}