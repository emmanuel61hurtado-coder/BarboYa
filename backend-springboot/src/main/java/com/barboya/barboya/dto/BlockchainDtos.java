package com.barboya.barboya.dto;

public final class BlockchainDtos {
    private BlockchainDtos() {}

    public record CryptoPaymentRequest(String txHash, String payerAddress, long tokenAmountUnits) {}
    public record CryptoPaymentView(java.util.UUID id, String txHash, String status, int amountCop, int confirmations) {}
}