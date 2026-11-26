package com.barboya.orders;

import com.barboya.common.ApiException;
import com.barboya.products.Product;
import com.barboya.products.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reglas comunes de cambio de estado: validacion, historial, existencias y avisos. */
@Component
class OrderLifecycle {

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderEventRepository events;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    OrderLifecycle(OrderEventRepository events, OrderItemRepository items, ProductRepository products,
                   ApplicationEventPublisher publisher, Clock clock) {
        this.events = events;
        this.items = items;
        this.products = products;
        this.publisher = publisher;
        this.clock = clock;
    }

    int maxPinAttempts() {
        return MAX_PIN_ATTEMPTS;
    }

    String newPin() {
        return "%04d".formatted(RANDOM.nextInt(10_000));
    }

    void require(CustomerOrder order, OrderStatus... allowed) {
        if (!Arrays.asList(allowed).contains(order.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "El pedido ya cambio de estado; actualiza la pantalla");
        }
    }

    void move(CustomerOrder order, OrderStatus next, UUID merchantId, UUID actorId, String note) {
        order.setStatus(next);
        if (next == OrderStatus.REJECTED || next == OrderStatus.CANCELLED) {
            order.setStatusReason(note);
        }
        record(order, merchantId, actorId, note);
    }

    /** Anota el hecho y avisa en vivo cuando la transaccion quede confirmada. */
    void record(CustomerOrder order, UUID merchantId, UUID actorId, String note) {
        Instant now = clock.instant();
        order.setUpdatedAt(now);
        events.save(new OrderEvent(order.getId(), order.getStatus(), actorId, note, now));
        publisher.publishEvent(new OrderChanged(order.getId(), order.getCustomerId(), merchantId,
                order.getCourierId(), order.getFulfillment() == CustomerOrder.Fulfillment.COURIER));
    }

    /** Quita el domiciliario para que otra persona pueda tomar el pedido. */
    void unassign(CustomerOrder order, UUID merchantId, UUID actorId, String note) {
        order.setCourierId(null);
        order.setAssignedAt(null);
        order.setPickupCode(null);
        order.setPickupAttempts(0);
        record(order, merchantId, actorId, note);
    }

    boolean matches(String expected, String received) {
        return expected != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), received.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Solo el PIN del cliente marca la entrega. Quien llame debe usar
     * {@code noRollbackFor} para que los intentos fallidos queden contados.
     */
    void confirmDelivery(CustomerOrder order, UUID merchantId, UUID actorId, String pin) {
        if (order.getPinAttempts() >= MAX_PIN_ATTEMPTS) {
            throw new ApiException(HttpStatus.CONFLICT, "PIN_LOCKED",
                    "Demasiados intentos. Pide al cliente que genere un PIN nuevo");
        }
        if (!matches(order.getDeliveryPin(), pin)) {
            order.setPinAttempts(order.getPinAttempts() + 1);
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PIN", "PIN incorrecto");
        }
        order.setDeliveredAt(clock.instant());
        move(order, OrderStatus.DELIVERED, merchantId, actorId, "Entrega confirmada con PIN");
    }

    /** Devuelve al inventario lo reservado por un pedido que no se entregara. */
    void releaseStock(CustomerOrder order) {
        List<OrderItem> lines = items.findByOrderIdInOrderByName(List.of(order.getId()));
        Map<UUID, Product> locked = products.lockAll(lines.stream().map(OrderItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Instant now = clock.instant();
        for (OrderItem line : lines) {
            Product product = locked.get(line.getProductId());
            product.putBack(line.getQuantity());
            product.setUpdatedAt(now);
        }
    }

    static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Pedido no encontrado");
    }
}
