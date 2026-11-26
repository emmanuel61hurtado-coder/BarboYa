package com.barboya.barboya.dto;

public final class SecurityDtos {
    private SecurityDtos() {}

    public record SosRequest(java.util.UUID tripId, double lat, double lng, String details) {}
    public record StoppedRequest(java.util.UUID tripId, double lat, double lng) {}
    public record ShareLinkResponse(String token) {}
}