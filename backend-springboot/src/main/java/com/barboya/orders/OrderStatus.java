package com.barboya.orders;

import java.util.Set;

public enum OrderStatus {
    PLACED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    REJECTED,
    CANCELLED,
    /** Recogido por un domiciliario y no entregado. */
    FAILED;

    public static final Set<OrderStatus> ACTIVE = Set.of(PLACED, PREPARING, READY, OUT_FOR_DELIVERY);
    public static final Set<OrderStatus> FINISHED = Set.of(DELIVERED, REJECTED, CANCELLED, FAILED);
}
