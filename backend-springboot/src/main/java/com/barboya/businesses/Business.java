package com.barboya.businesses;

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
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business {

    public enum Category {
        RESTAURANT,
        GROCERY,
        PHARMACY,
        BAKERY,
        OTHER
    }

    @Id
    private UUID id;
    private UUID ownerId;
    private String name;
    private String legalName;
    private String nit;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String description;
    private String address;
    private String phone;
    private boolean pickupEnabled;
    private boolean deliveryEnabled;
    private int deliveryFeeCop;
    private boolean acceptingOrders;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    static Business create(UUID ownerId, Instant now) {
        Business business = new Business();
        business.id = UUID.randomUUID();
        business.ownerId = ownerId;
        business.createdAt = now;
        business.updatedAt = now;
        return business;
    }
}
