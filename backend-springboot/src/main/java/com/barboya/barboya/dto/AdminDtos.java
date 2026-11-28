package com.barboya.barboya.dto;

public final class AdminDtos {
    private AdminDtos() {}

    public record BillingUpdateRequest(java.util.UUID driverId, String scheme, int tripFeeOverrideCop) {}
    public record TopupDecisionRequest(boolean approve) {}
    public record SubscriptionActivateRequest(java.util.UUID driverId, String plan) {}
    public record DriverView(java.util.UUID id, String fullName, String billingScheme, double walletBalanceCop) {}
}