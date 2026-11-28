package com.barboya.orders;

import java.time.Instant;

final class OrderRules {

    private OrderRules() {
    }

    /**
     * Antes de que el comercio lo acepte, siempre. Despues, solo si se vencio el
     * plazo de entrega prometido (Ley 1480 de 2011, articulo 50, literal h).
     */
    static boolean customerCanCancel(CustomerOrder order, Instant now) {
        return order.getStatus() == OrderStatus.PLACED
                || (OrderStatus.ACTIVE.contains(order.getStatus())
                && order.getDeliverBy() != null && now.isAfter(order.getDeliverBy()));
    }
}
