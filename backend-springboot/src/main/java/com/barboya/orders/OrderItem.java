package com.barboya.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    private UUID id;
    private UUID orderId;
    private UUID productId;
    private String name;
    private int unitPriceCop;
    private int quantity;
    private int lineTotalCop;

    OrderItem(UUID orderId, UUID productId, String name, int unitPriceCop, int quantity) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.productId = productId;
        this.name = name;
        this.unitPriceCop = unitPriceCop;
        this.quantity = quantity;
        this.lineTotalCop = Math.multiplyExact(unitPriceCop, quantity);
    }
}
