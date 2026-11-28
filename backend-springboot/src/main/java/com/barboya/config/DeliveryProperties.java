package com.barboya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Reglas de las entregas con domiciliarios. Las distancias son en linea recta. */
@ConfigurationProperties(prefix = "barboya.delivery")
public record DeliveryProperties(
        @DefaultValue("3000") int baseFeeCop,
        @DefaultValue("1000") int perKmCop,
        @DefaultValue("8") double maxDistanceKm,
        @DefaultValue("5") double pickupRadiusKm,
        /* Recorrido total (hasta el comercio y hasta el cliente) admitido en bicicleta */
        @DefaultValue("4") double bicycleMaxKm,
        @DefaultValue("2m") Duration locationMaxAge) {
}
