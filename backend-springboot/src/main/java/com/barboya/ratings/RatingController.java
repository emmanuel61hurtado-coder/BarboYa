package com.barboya.ratings;

import com.barboya.common.ApiException;
import com.barboya.ratings.RatingDtos.NewRating;
import com.barboya.ratings.RatingDtos.OrderRatings;
import com.barboya.ratings.RatingDtos.Reviews;
import com.barboya.ratings.RatingDtos.Summary;
import com.barboya.security.AuthUser;
import com.barboya.users.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class RatingController {

    private final RatingService service;

    public RatingController(RatingService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/orders/{orderId}/ratings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderRatings forOrder(@AuthenticationPrincipal AuthUser user, @PathVariable UUID orderId) {
        return service.forOrder(user.id(), orderId);
    }

    @PostMapping("/api/v1/orders/{orderId}/ratings")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderRatings rate(@AuthenticationPrincipal AuthUser user, @PathVariable UUID orderId,
                             @Valid @RequestBody NewRating body) {
        return service.rate(user.id(), orderId, body);
    }

    /** Resumen de varios comercios, para las listas del catalogo. */
    @GetMapping("/api/v1/ratings/businesses")
    public Map<UUID, Summary> summaries(@RequestParam List<UUID> ids) {
        if (ids.size() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_IDS", "Consulta maximo 100 comercios");
        }
        return service.summaries(Rating.Target.BUSINESS, ids);
    }

    @GetMapping("/api/v1/ratings/businesses/{id}")
    public Reviews reviews(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page) {
        return service.publicReviews(id, page);
    }

    @GetMapping("/api/v1/ratings/me")
    @PreAuthorize("hasAnyRole('RESTAURANT','STORE','COURIER')")
    public Reviews received(@AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "0") int page) {
        Rating.Target type = user.role() == Role.COURIER ? Rating.Target.COURIER : Rating.Target.BUSINESS;
        return service.received(type, user.id(), page);
    }
}
