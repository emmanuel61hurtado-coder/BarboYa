package com.barboya.businesses;

import com.barboya.products.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class BusinessDtos {

    private BusinessDtos() {
    }

    public record BusinessRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @Size(max = 160) String legalName,
            @Pattern(regexp = "^$|^[0-9.]{6,15}(-[0-9])?$", message = "NIT no valido") String nit,
            @NotNull Business.Category category,
            @Size(max = 500) String description,
            @NotBlank @Size(min = 5, max = 200) String address,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,16}$", message = "Telefono no valido") String phone,
            @NotNull Boolean pickupEnabled,
            @NotNull Boolean deliveryEnabled,
            @NotNull @Min(0) @Max(100000) Integer deliveryFeeCop,
            Boolean courierDeliveryEnabled,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude) {
    }

    public record AcceptingRequest(@NotNull Boolean accepting) {
    }

    /** Reemplaza todo el horario; una lista vacia lo quita. */
    public record HoursRequest(@NotNull @Size(max = 14) List<@NotNull @Valid OpeningHours> hours) {
    }

    public record ProductRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 60) String category,
            @NotNull @Min(100) @Max(10000000) Integer priceCop,
            @Min(0) @Max(100000) Integer stock,
            @NotNull Boolean available) {
    }

    public record PromotionRequest(
            @NotNull @Min(100) @Max(10000000) Integer priceCop,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn) {
    }

    /** {@code active}: si el precio rebajado se cobra hoy. */
    public record Promotion(int priceCop, LocalDate startsOn, LocalDate endsOn, boolean active) {

        static Promotion from(Product p, LocalDate today) {
            return p.getPromoPriceCop() == null ? null : new Promotion(p.getPromoPriceCop(), p.getPromoStartsOn(),
                    p.getPromoEndsOn(), p.onPromotion(today));
        }
    }

    /** Vista del propietario y del administrador: incluye los datos legales. */
    public record BusinessResponse(
            UUID id, String name, String legalName, String nit, Business.Category category, String description,
            String address, String phone, boolean pickupEnabled, boolean deliveryEnabled, int deliveryFeeCop,
            boolean acceptingOrders, boolean courierDeliveryEnabled, Double latitude, Double longitude,
            List<OpeningHours> hours) {

        static BusinessResponse from(Business b) {
            return new BusinessResponse(b.getId(), b.getName(), b.getLegalName(), b.getNit(), b.getCategory(),
                    b.getDescription(), b.getAddress(), b.getPhone(), b.isPickupEnabled(), b.isDeliveryEnabled(),
                    b.getDeliveryFeeCop(), b.isAcceptingOrders(), b.isCourierDeliveryEnabled(),
                    b.getLatitude(), b.getLongitude(), b.schedule());
        }
    }

    public record ProductResponse(
            UUID id, String name, String description, String category, int priceCop, Integer stock,
            boolean available, String photoUrl, Promotion promotion) {

        static ProductResponse from(Product p, LocalDate today) {
            return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getCategory(),
                    p.getPriceCop(), p.getStock(), p.isAvailable(), p.photoUrl(),
                    Promotion.from(p, today));
        }
    }

    /** Lo que ve un cliente al explorar. */
    public record StoreSummary(
            UUID id, String name, Business.Category category, String description, String address,
            boolean pickupEnabled, boolean deliveryEnabled, int deliveryFeeCop, boolean open,
            boolean courierDeliveryEnabled, Double latitude, Double longitude) {

        /** @param open si recibe pedidos en este momento */
        static StoreSummary from(Business b, boolean open) {
            return new StoreSummary(b.getId(), b.getName(), b.getCategory(), b.getDescription(), b.getAddress(),
                    b.isPickupEnabled(), b.isDeliveryEnabled(), b.getDeliveryFeeCop(),
                    open, b.isCourierDeliveryEnabled(), b.getLatitude(), b.getLongitude());
        }
    }

    /** Del inventario solo se publica si hay existencias, no cuantas. */
    public record MenuItem(
            UUID id, String name, String description, String category, int priceCop, boolean soldOut,
            String photoUrl, Integer regularPriceCop, LocalDate promoEndsOn) {

        /** {@code priceCop} es lo que se cobra hoy; con rebaja vienen ademas el precio normal y hasta cuando. */
        static MenuItem from(Product p, LocalDate today) {
            boolean promo = p.onPromotion(today);
            return new MenuItem(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.priceOn(today),
                    !p.canSell(1), p.photoUrl(), promo ? p.getPriceCop() : null,
                    promo ? p.getPromoEndsOn() : null);
        }
    }

    public record Store(StoreSummary business, List<OpeningHours> hours, List<MenuItem> products) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }
}
