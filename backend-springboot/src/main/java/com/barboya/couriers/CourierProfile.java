package com.barboya.couriers;

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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courier_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierProfile {

    @Id
    private UUID userId;
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    private String plate;
    private Instant consentAt;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    static CourierProfile create(UUID userId, Instant now) {
        CourierProfile profile = new CourierProfile();
        profile.userId = userId;
        profile.consentAt = now;
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }
}
