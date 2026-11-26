package com.barboya.common;

public final class Geo {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private Geo() {
    }

    public record Point(double latitude, double longitude) {
    }

    /** Distancia en linea recta (formula del semiverseno). */
    public static double distanceKm(Point from, Point to) {
        double latitude = Math.toRadians(to.latitude() - from.latitude());
        double longitude = Math.toRadians(to.longitude() - from.longitude());
        double a = Math.pow(Math.sin(latitude / 2), 2)
                + Math.cos(Math.toRadians(from.latitude())) * Math.cos(Math.toRadians(to.latitude()))
                * Math.pow(Math.sin(longitude / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public static double round(double km) {
        return Math.round(km * 100) / 100.0;
    }
}
