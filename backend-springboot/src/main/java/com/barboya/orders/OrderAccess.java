package com.barboya.orders;

import com.barboya.businesses.BusinessRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Lo que otros modulos pueden preguntar sobre quien participa en un pedido. */
@Component
@Transactional(readOnly = true)
public class OrderAccess {

    private final OrderRepository orders;
    private final BusinessRepository businesses;

    OrderAccess(OrderRepository orders, BusinessRepository businesses) {
        this.orders = orders;
        this.businesses = businesses;
    }

    /** {@code courierId} es nulo si no hubo domiciliario de BarboYa. */
    public record Parties(UUID customerId, UUID businessId, UUID courierId, boolean delivered, Instant deliveredAt) {
    }

    public Optional<Parties> parties(UUID orderId) {
        return orders.findById(orderId).map(o -> new Parties(o.getCustomerId(), o.getBusinessId(), o.getCourierId(),
                o.getStatus() == OrderStatus.DELIVERED, o.getDeliveredAt()));
    }

    /** Pedidos sin terminar en los que participa, como cliente, comercio o domiciliario. */
    public boolean hasActiveOrders(UUID userId) {
        return orders.countParticipating(userId, OrderStatus.ACTIVE) > 0;
    }

    /** Cliente, dueno del comercio o domiciliario asignado. */
    public boolean participates(UUID orderId, UUID userId) {
        return orders.findById(orderId)
                .filter(o -> userId.equals(o.getCustomerId()) || userId.equals(o.getCourierId())
                        || businesses.findById(o.getBusinessId())
                                .filter(b -> b.getOwnerId().equals(userId)).isPresent())
                .isPresent();
    }
}
