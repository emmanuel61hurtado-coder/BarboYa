package com.barboya.businesses;

import com.barboya.common.ApiException;
import com.barboya.common.Geo;
import com.barboya.config.DeliveryProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Tarifa de un domiciliario de BarboYa: base mas valor por kilometro, a la centena. */
@Component
public class DeliveryPricing {

    public record Quote(double distanceKm, int feeCop) {
    }

    private final DeliveryProperties settings;

    public DeliveryPricing(DeliveryProperties settings) {
        this.settings = settings;
    }

    public Quote quote(Business business, Geo.Point destination) {
        if (!business.isCourierDeliveryEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "FULFILLMENT_UNAVAILABLE",
                    "El comercio no ofrece esa forma de entrega");
        }
        double km = Geo.round(Geo.distanceKm(
                new Geo.Point(business.getLatitude(), business.getLongitude()), destination));
        if (km > settings.maxDistanceKm()) {
            throw new ApiException(HttpStatus.CONFLICT, "OUT_OF_RANGE",
                    "La direccion esta fuera del area de entrega de este comercio");
        }
        int fee = settings.baseFeeCop() + (int) Math.round(km * settings.perKmCop() / 100.0) * 100;
        return new Quote(km, fee);
    }
}
