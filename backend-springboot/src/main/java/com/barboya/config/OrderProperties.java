package com.barboya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** {@code acceptTimeout}: cuanto tiene el comercio para responder un pedido. */
@ConfigurationProperties(prefix = "barboya.orders")
public record OrderProperties(@DefaultValue("15m") Duration acceptTimeout) {
}
