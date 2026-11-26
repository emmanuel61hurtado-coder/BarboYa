package com.barboya.barboya.dto;

public final class MerchantDtos {
    private MerchantDtos() {}

    public record CreateMerchantRequest(String name, String address, double lat, double lng, String category) {}
    public record CreateProductRequest(String name, String description, int priceCop) {}
    public record CreateOrderRequest(java.util.UUID merchantId, String notes, int totalCop) {}
    public record MerchantView(java.util.UUID id, String name, String status) {}
    public record ProductView(java.util.UUID id, String name, int priceCop, boolean available) {}
}