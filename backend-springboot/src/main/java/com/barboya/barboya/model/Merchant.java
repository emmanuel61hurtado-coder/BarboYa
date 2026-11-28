package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
public class Merchant {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    private String name;
    private String address;
    private double lat;
    private double lng;

    @Enumerated(EnumType.STRING)
    private Enums.MerchantCategory category = Enums.MerchantCategory.other;

    @Enumerated(EnumType.STRING)
    private Enums.MerchantStatus status = Enums.MerchantStatus.pending_approval;

    private boolean active = true;
}