package com.barboya.orders;

import com.barboya.businesses.BusinessRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    /** Cliente, dueno del comercio o domiciliario asignado. */
    public boolean participates(UUID orderId, UUID userId) {
        return orders.findById(orderId)
                .filter(o -> userId.equals(o.getCustomerId()) || userId.equals(o.getCourierId())
                        || businesses.findById(o.getBusinessId())
                                .filter(b -> b.getOwnerId().equals(userId)).isPresent())
                .isPresent();
    }
}
