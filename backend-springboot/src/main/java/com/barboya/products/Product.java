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
import java.time.LocalDate;
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
    /** Clave del archivo de la foto; nula si no tiene. */
    private String photoKey;
    /** Los tres van juntos: sin promocion son nulos. */
    private Integer promoPriceCop;
    private LocalDate promoStartsOn;
    private LocalDate promoEndsOn;

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

    public boolean onPromotion(LocalDate day) {
        return promoPriceCop != null && !day.isBefore(promoStartsOn) && !day.isAfter(promoEndsOn);
    }

    /** Precio que se cobra ese dia. */
    public int priceOn(LocalDate day) {
        return onPromotion(day) ? promoPriceCop : priceCop;
    }

    public void promote(int price, LocalDate startsOn, LocalDate endsOn) {
        promoPriceCop = price;
        promoStartsOn = startsOn;
        promoEndsOn = endsOn;
    }

    public void endPromotion() {
        promoPriceCop = null;
        promoStartsOn = null;
        promoEndsOn = null;
    }

    public String photoUrl() {
        return photoKey == null ? null : "/api/v1/photos/" + photoKey;
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
