package com.barboya.orders;

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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerOrder {

    public enum Fulfillment {
        PICKUP,
        /** Lo lleva el personal del comercio. */
        DELIVERY,
        /** Lo lleva un domiciliario de BarboYa. */
        COURIER
    }

    public enum PaymentMethod {
        CASH
    }

    @Id
    private UUID id;

    /** Numero corto que asigna la base, para hablar del pedido. */
    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Long number;

    private UUID customerId;
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private Fulfillment fulfillment;

    private String deliveryAddress;
    private String notes;
    private int subtotalCop;
    private int deliveryFeeCop;
    private int totalCop;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String deliveryPin;
    private int pinAttempts;
    private Integer prepMinutes;
    private String statusReason;
    private String idempotencyKey;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deliveredAt;
    /** Pasada esta hora sin entregar, el cliente puede terminar el pedido. */
    private Instant deliverBy;

    private UUID courierId;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private Double distanceKm;
    private String pickupCode;
    private int pickupAttempts;
    private Instant assignedAt;
    private Instant pickedUpAt;
    private String evidenceKey;
    private String evidenceType;
    private int merchantCommissionCop;
    private int courierCommissionCop;

    @Version
    private Long version;

    static CustomerOrder create(UUID customerId, UUID businessId, String idempotencyKey, Instant now) {
        CustomerOrder order = new CustomerOrder();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.businessId = businessId;
        order.idempotencyKey = idempotencyKey;
        order.status = OrderStatus.PLACED;
        order.paymentMethod = PaymentMethod.CASH;
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }
}
