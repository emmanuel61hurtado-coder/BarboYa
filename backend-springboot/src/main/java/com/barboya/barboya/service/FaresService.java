package com.barboya.barboya.service;

import com.barboya.barboya.config.BarboyaProperties;
import org.springframework.stereotype.Service;

@Service
public class FaresService {
    private final BarboyaProperties props;

    public FaresService(BarboyaProperties props) {
        this.props = props;
    }

    public double distanceKm(double oLat, double oLng, double dLat, double dLng) {
        // Haversine
        double R = 6371.0;
        double dLatR = Math.toRadians(dLat - oLat);
        double dLngR = Math.toRadians(dLng - oLng);
        double a = Math.sin(dLatR / 2) * Math.sin(dLatR / 2)
                + Math.cos(Math.toRadians(oLat)) * Math.cos(Math.toRadians(dLat))
                * Math.sin(dLngR / 2) * Math.sin(dLngR / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public int suggestedFare(double oLat, double oLng, double dLat, double dLng) {
        double km = distanceKm(oLat, oLng, dLat, dLng);
        int base = 3000;
        int perKm = 800;
        int fare = base + (int) (km * perKm);
        // night surcharge not computed here; keep simple
        return fare;
    }
}