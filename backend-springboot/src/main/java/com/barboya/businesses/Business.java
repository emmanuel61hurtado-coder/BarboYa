package com.barboya.businesses;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

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
    private boolean courierDeliveryEnabled;
    private Double latitude;
    private Double longitude;

    /** Vacio: no usa horario. */
    @ElementCollection
    @CollectionTable(name = "business_hours", joinColumns = @JoinColumn(name = "business_id"))
    @BatchSize(size = 50)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Set<OpeningHours> hours = new HashSet<>();

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    /** Los horarios son hora de Colombia. */
    public boolean isOpenAt(Instant moment) {
        return OpeningHours.covers(hours, LocalDateTime.ofInstant(moment, ZONE));
    }

    List<OpeningHours> schedule() {
        return hours.stream().sorted(OpeningHours.ORDER).toList();
    }

    void replaceHours(List<OpeningHours> week) {
        OpeningHours.validate(week);
        hours.clear();
        hours.addAll(week);
    }

    static Business create(UUID ownerId, Instant now) {
        Business business = new Business();
        business.id = UUID.randomUUID();
        business.ownerId = ownerId;
        business.createdAt = now;
        business.updatedAt = now;
        return business;
    }
}
