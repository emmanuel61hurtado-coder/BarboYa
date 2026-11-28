package com.barboya.orders;

import java.util.UUID;

/** Un domiciliario no pudo entregar un pedido que ya habia recogido. */
public record DeliveryFailed(UUID orderId, long orderNumber, UUID courierId, String reason) {
}
