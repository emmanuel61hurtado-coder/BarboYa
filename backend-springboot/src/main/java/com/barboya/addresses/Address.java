package com.barboya.addresses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Id
    private UUID id;
    @Column(updatable = false)
    private UUID userId;
    private String label;
    private String address;
    private String notes;
    private Double latitude;
    private Double longitude;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    Address(UUID userId, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.createdAt = now;
    }

    void update(String label, String address, String notes, Double latitude, Double longitude, Instant now) {
        this.label = label;
        this.address = address;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = now;
    }
}
