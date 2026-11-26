package com.barboya.barboya.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "zones")
@Getter
@Setter
public class Zone {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    private String name;
    private boolean urban = true;
    @Column(columnDefinition = "text")
    private String boundary; // WKT / geometry as text
    private int baseFareCop;
    private int perKmCop;
    private int nightSurchargePct;
    private String billingScheme = "per_trip_fee";
    private int perTripFeeCop;
}