package com.barboya.orders;

import java.util.UUID;

/** Un pedido cambio: avisar a quienes participan en el. */
public record OrderChanged(UUID orderId, UUID customerId, UUID merchantId) {
}
