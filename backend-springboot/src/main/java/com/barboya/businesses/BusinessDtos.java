package com.barboya.businesses;

import com.barboya.products.Product;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
            @NotNull @Min(0) @Max(100000) Integer deliveryFeeCop) {
    }

    public record AcceptingRequest(@NotNull Boolean accepting) {
    }

    public record ProductRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 60) String category,
            @NotNull @Min(100) @Max(10000000) Integer priceCop,
            @Min(0) @Max(100000) Integer stock,
            @NotNull Boolean available) {
    }

    /** Vista del propietario y del administrador: incluye los datos legales. */
    public record BusinessResponse(
            UUID id, String name, String legalName, String nit, Business.Category category, String description,
            String address, String phone, boolean pickupEnabled, boolean deliveryEnabled, int deliveryFeeCop,
            boolean acceptingOrders) {

        static BusinessResponse from(Business b) {
            return new BusinessResponse(b.getId(), b.getName(), b.getLegalName(), b.getNit(), b.getCategory(),
                    b.getDescription(), b.getAddress(), b.getPhone(), b.isPickupEnabled(), b.isDeliveryEnabled(),
                    b.getDeliveryFeeCop(), b.isAcceptingOrders());
        }
    }

    public record ProductResponse(
            UUID id, String name, String description, String category, int priceCop, Integer stock,
            boolean available) {

        static ProductResponse from(Product p) {
            return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getCategory(),
                    p.getPriceCop(), p.getStock(), p.isAvailable());
        }
    }

    /** Lo que ve un cliente al explorar. */
    public record StoreSummary(
            UUID id, String name, Business.Category category, String description, String address,
            boolean pickupEnabled, boolean deliveryEnabled, int deliveryFeeCop, boolean open) {

        static StoreSummary from(Business b) {
            return new StoreSummary(b.getId(), b.getName(), b.getCategory(), b.getDescription(), b.getAddress(),
                    b.isPickupEnabled(), b.isDeliveryEnabled(), b.getDeliveryFeeCop(), b.isAcceptingOrders());
        }
    }

    /** Del inventario solo se publica si hay existencias, no cuantas. */
    public record MenuItem(
            UUID id, String name, String description, String category, int priceCop, boolean soldOut) {

        static MenuItem from(Product p) {
            return new MenuItem(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getPriceCop(),
                    !p.canSell(1));
        }
    }

    public record Store(StoreSummary business, List<MenuItem> products) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }
}
