package com.barboya.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SupportDtos {

    private SupportDtos() {
    }

    public record NewTicket(
            @NotNull Ticket.Category category,
            @NotBlank @Size(min = 5, max = 120) String subject,
            @NotBlank @Size(min = 10, max = 2000) String message,
            UUID orderId) {
    }

    public record Reply(@NotBlank @Size(min = 2, max = 2000) String body) {
    }

    public record Resolution(@NotBlank @Size(min = 10, max = 500) String resolution) {
    }

    /** {@code opener} solo viaja al equipo. */
    public record Opener(String name, String phone, String email, String role) {
    }

    public record TicketSummary(
            UUID id, long number, Ticket.Category category, String subject, Ticket.Status status, UUID orderId,
            Instant createdAt, Instant updatedAt) {

        static TicketSummary from(Ticket t) {
            return new TicketSummary(t.getId(), t.getNumber(), t.getCategory(), t.getSubject(), t.getStatus(),
                    t.getOrderId(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    /** En las respuestas del equipo no se revela que persona escribio. */
    public record Message(boolean staff, String body, Instant at) {
    }

    public record TicketView(TicketSummary ticket, Opener opener, String resolution, List<Message> messages) {
    }

    public record TicketPage(List<TicketSummary> items, int page, int size, long total) {
    }
}
