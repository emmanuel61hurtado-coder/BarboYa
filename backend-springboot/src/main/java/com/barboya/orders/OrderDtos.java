package com.barboya.orders;

import com.barboya.common.Geo;
import com.barboya.couriers.VehicleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record LineRequest(@NotNull UUID productId, @NotNull @Min(1) @Max(50) Integer quantity) {
    }

    /** {@code expectedTotalCop} es el total que el cliente vio y acepto. */
    public record PlaceOrderRequest(
            @NotNull UUID businessId,
            @NotEmpty @Size(max = 30) List<@Valid @NotNull LineRequest> items,
            @NotNull CustomerOrder.Fulfillment fulfillment,
            @Size(max = 200) String deliveryAddress,
            @Size(max = 300) String notes,
            @NotNull CustomerOrder.PaymentMethod paymentMethod,
            @NotNull Integer expectedTotalCop,
            @DecimalMin("-90") @DecimalMax("90") Double deliveryLatitude,
            @DecimalMin("-180") @DecimalMax("180") Double deliveryLongitude) {
    }

    public record PickupRequest(
            @NotBlank @Pattern(regexp = "^\\d{4}$", message = "El codigo tiene 4 digitos") String code) {
    }

    /** Un pedido disponible, sin datos personales del cliente. */
    public record DeliveryOffer(
            UUID id, long number, OrderStatus status, String businessName, String businessAddress,
            double pickupDistanceKm, double deliveryDistanceKm, int feeCop, int commissionCop,
            int payAtPickupCop, int collectCop) {
    }

    /** {@code location} es nula si no hay una posicion reciente. */
    public record CourierInfo(String name, String phone, VehicleType vehicleType, String plate, Geo.Point location) {
    }

    public record Evidence(byte[] content, String contentType) {
    }

    public record Earnings(long todayCop, long last7DaysCop, long last30DaysCop) {
    }

    public record AcceptRequest(@NotNull @Min(1) @Max(240) Integer prepMinutes) {
    }

    public record ReasonRequest(@NotBlank @Size(min = 5, max = 300) String reason) {
    }

    public record DeliverRequest(@NotBlank @Pattern(regexp = "^\\d{4}$", message = "El PIN tiene 4 digitos") String pin) {
    }

    public record Line(UUID productId, String name, int unitPriceCop, int quantity, int lineTotalCop) {
    }

    public record Step(OrderStatus status, String note, Instant at) {
    }

    public record Contact(String name, String phone) {
    }

    /**
     * {@code deliveryPin} solo viaja al cliente y {@code customer} solo al
     * comercio, ambos mientras el pedido siga en curso.
     */
    public record OrderResponse(
            UUID id, long number, OrderStatus status, CustomerOrder.Fulfillment fulfillment,
            UUID businessId, String businessName, String businessPhone, String businessAddress,
            Geo.Point businessLocation, Contact customer, String deliveryAddress, Geo.Point deliveryLocation,
            Double distanceKm, String notes, List<Line> items,
            int subtotalCop, int deliveryFeeCop, int totalCop, CustomerOrder.PaymentMethod paymentMethod,
            Integer prepMinutes, String statusReason, String deliveryPin, boolean pinLocked,
            String pickupCode, CourierInfo courier, boolean hasEvidence, Integer commissionCop,
            Instant createdAt, Instant deliveredAt, List<Step> history) {
    }

    public record OrderPage(List<OrderResponse> items, int page, int size, long total) {
    }
}
