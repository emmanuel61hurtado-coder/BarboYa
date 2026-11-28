package com.barboya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Servicios apagados hasta cumplir sus requisitos legales (docs/revision-legal.md):
 * farmacias y registro de quienes transportan pasajeros.
 */
@ConfigurationProperties(prefix = "barboya.features")
public record FeatureProperties(
        @DefaultValue("false") boolean pharmacy,
        @DefaultValue("false") boolean driverRegistration) {
}
