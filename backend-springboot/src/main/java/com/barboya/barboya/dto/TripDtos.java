package com.barboya.barboya.dto;

import com.barboya.barboya.enums.Enums;

public final class TripDtos {
    private TripDtos() {}

    public record QuoteRequest(double originLat, double originLng, double destLat, double destLng, Enums.ServiceType serviceType) {}
    public record QuoteResponse(int suggestedFareCop, double distanceKm) {}
    public record CreateTripRequest(double originLat, double originLng, String originAddress,
                                    double destLat, double destLng, String destAddress,
                                    Enums.ServiceType serviceType, Enums.PaymentMethod paymentMethod, int agreedFareCop) {}
    public record OfferRequest(int fareCop) {}
    public record AcceptOfferRequest(java.util.UUID offerId) {}
    public record VerifyPinRequest(int pin) {}
    public record TripView(java.util.UUID id, String status, int agreedFareCop, java.util.UUID customerId, java.util.UUID driverId, String shareToken) {}
}