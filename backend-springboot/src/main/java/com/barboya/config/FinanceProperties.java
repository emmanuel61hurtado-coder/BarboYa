package com.barboya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Comisiones en puntos basicos (1000 = 10 %). El tope es la deuda a partir de la
 * cual no se reciben pedidos nuevos.
 */
@ConfigurationProperties(prefix = "barboya.finance")
public record FinanceProperties(
        @DefaultValue("1000") int merchantCommissionBps,
        @DefaultValue("1000") int courierCommissionBps,
        @DefaultValue("50000") int debtLimitCop) {

    public FinanceProperties {
        if (merchantCommissionBps < 0 || merchantCommissionBps > 5000
                || courierCommissionBps < 0 || courierCommissionBps > 5000 || debtLimitCop < 0) {
            throw new IllegalArgumentException("Configuracion de comisiones fuera de rango");
        }
    }

    public int merchantCommission(int subtotalCop) {
        return share(subtotalCop, merchantCommissionBps);
    }

    public int courierCommission(int deliveryFeeCop) {
        return share(deliveryFeeCop, courierCommissionBps);
    }

    /** Redondeo al peso mas cercano, sin coma flotante. */
    private static int share(int amount, int bps) {
        return Math.toIntExact(((long) amount * bps + 5000) / 10000);
    }
}
