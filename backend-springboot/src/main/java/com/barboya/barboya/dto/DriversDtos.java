package com.barboya.barboya.dto;

public final class DriversDtos {
    private DriversDtos() {}

    public record LocationUpdate(double lat, double lng) {}
    public record OnlineToggle(boolean online) {}
}