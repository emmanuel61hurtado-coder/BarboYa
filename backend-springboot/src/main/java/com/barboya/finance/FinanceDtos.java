package com.barboya.finance;

import com.barboya.finance.LedgerEntry.Kind;
import com.barboya.finance.LedgerEntry.Method;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FinanceDtos {

    private FinanceDtos() {
    }

    public record PaymentRequest(
            @NotNull @Min(1) Integer amountCop,
            @NotNull Method method,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9 ._#-]{4,80}$", message = "Referencia no valida")
            String reference,
            @Size(max = 300) String note) {
    }

    public record AdjustmentRequest(
            @NotNull @Min(1) Integer amountCop,
            @NotBlank @Size(min = 10, max = 300) String reason) {
    }

    public record ReversalRequest(@NotBlank @Size(min = 10, max = 300) String reason) {
    }

    /** {@code effectCop}: positivo aumenta la deuda y negativo la reduce. */
    public record Movement(
            UUID id, Kind kind, int effectCop, UUID orderId, Method method, String reference, String note,
            boolean reversed, Instant at) {
    }

    public record Statement(
            long balanceCop, int debtLimitCop, boolean blocked, int commissionBps,
            List<Movement> movements, int page, int size, long total) {
    }

    public record Period(long orders, long salesCop, long commissionsCop) {
    }

    public record DebtorView(UUID partyId, String name, String role, long balanceCop, boolean blocked) {
    }

    /** {@code outstandingCop} es lo que falta por cobrar y {@code collectedCop} lo recibido. */
    public record Summary(
            Period today, Period last7Days, Period last30Days,
            long outstandingCop, long collectedCop, long waivedCop, List<DebtorView> debtors) {
    }
}
