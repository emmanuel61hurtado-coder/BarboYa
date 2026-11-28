package com.barboya.orders;

import com.barboya.businesses.BusinessRepository;
import com.barboya.config.OrderProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Cancela los pedidos que el comercio no respondio a tiempo y devuelve las existencias. */
@Service
public class OrderExpiryService {

    static final String REASON = "El comercio no respondio a tiempo";

    private final OrderRepository orders;
    private final BusinessRepository businesses;
    private final OrderLifecycle lifecycle;
    private final OrderProperties settings;
    private final Clock clock;

    OrderExpiryService(OrderRepository orders, BusinessRepository businesses, OrderLifecycle lifecycle,
                       OrderProperties settings, Clock clock) {
        this.orders = orders;
        this.businesses = businesses;
        this.lifecycle = lifecycle;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UUID> overdue() {
        return orders.findIdsCreatedBefore(OrderStatus.PLACED, cutoff(), PageRequest.of(0, 100));
    }

    /**
     * Una transaccion por pedido: se vuelve a comprobar con la fila bloqueada, por
     * si el comercio respondio mientras tanto o hay otra instancia haciendolo.
     */
    @Transactional
    public boolean expire(UUID orderId) {
        CustomerOrder order = orders.lockById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PLACED || !order.getCreatedAt().isBefore(cutoff())) {
            return false;
        }
        UUID merchant = businesses.findById(order.getBusinessId()).orElseThrow().getOwnerId();
        lifecycle.releaseStock(order);
        lifecycle.move(order, OrderStatus.CANCELLED, merchant, null, REASON);
        return true;
    }

    private Instant cutoff() {
        return clock.instant().minus(settings.acceptTimeout());
    }
}
