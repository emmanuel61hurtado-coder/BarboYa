package com.barboya.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "order_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEvent {

    @Id
    private UUID id;
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private UUID actorId;
    private String note;
    private Instant createdAt;

    OrderEvent(UUID orderId, OrderStatus status, UUID actorId, String note, Instant now) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.status = status;
        this.actorId = actorId;
        this.note = note;
        this.createdAt = now;
    }
}
