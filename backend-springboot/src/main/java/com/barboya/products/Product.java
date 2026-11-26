package com.barboya.products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    private UUID id;
    private UUID businessId;
    private String name;
    private String description;
    private String category;
    private int priceCop;
    private Integer stock;
    private boolean available;
    private boolean archived;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public static Product create(UUID businessId, Instant now) {
        Product product = new Product();
        product.id = UUID.randomUUID();
        product.businessId = businessId;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }

    public boolean canSell(int quantity) {
        return available && !archived && (stock == null || stock >= quantity);
    }

    public void take(int quantity) {
        if (stock != null) {
            stock -= quantity;
        }
    }

    public void putBack(int quantity) {
        if (stock != null) {
            stock += quantity;
        }
    }
}
