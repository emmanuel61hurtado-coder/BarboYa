package com.barboya.orders;

import java.util.UUID;

/**
 * Un pedido cambio: avisar a quienes participan en el. {@code offersChanged}
 * indica que la lista de pedidos disponibles para domiciliarios pudo cambiar.
 */
public record OrderChanged(UUID orderId, UUID customerId, UUID merchantId, UUID courierId,
                           boolean offersChanged) {
}
