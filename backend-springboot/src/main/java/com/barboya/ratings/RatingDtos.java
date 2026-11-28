package com.barboya.ratings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RatingDtos {

    private RatingDtos() {
    }

    public record NewRating(
            @NotNull Rating.Target target,
            @NotNull @Min(1) @Max(5) Integer stars,
            @Size(max = 500) String comment) {
    }

    public record Moderation(@NotBlank @Size(min = 10, max = 300) String reason) {
    }

    /** {@code average} es nulo cuando no hay calificaciones. */
    public record Summary(Double average, long count) {
    }

    public record Mine(int stars, String comment) {
    }

    public record TargetRating(Rating.Target target, Summary summary, Mine mine) {
    }

    /** {@code openUntil} indica hasta cuando se puede calificar; nulo si ya no se puede. */
    public record OrderRatings(Instant openUntil, List<TargetRating> targets) {
    }

    /** Sin autor ni pedido, para que no se pueda identificar a quien opino. */
    public record Review(int stars, String comment, Instant at) {

        static Review from(Rating r) {
            return new Review(r.getStars(), r.getComment(), r.getCreatedAt());
        }
    }

    public record Reviews(Summary summary, List<Review> items, int page, int size, long total) {
    }

    /** Vista del equipo. */
    public record Moderated(
            UUID id, UUID orderId, String authorName, Rating.Target target, String targetName,
            int stars, String comment, boolean hidden, String hiddenReason, Instant at) {
    }

    public record ModeratedPage(List<Moderated> items, int page, int size, long total) {
    }
}
