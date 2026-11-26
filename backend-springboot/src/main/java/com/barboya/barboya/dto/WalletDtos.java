package com.barboya.barboya.dto;

public final class WalletDtos {
    private WalletDtos() {}

    public record TopupRequest(int amountCop, String method) {}
    public record DecideTopupRequest(boolean approve) {}
    public record SubscriptionPurchaseRequest(String plan) {}
    public record WalletView(double balanceCop) {}
    public record TopupView(java.util.UUID id, int amountCop, String status) {}
}