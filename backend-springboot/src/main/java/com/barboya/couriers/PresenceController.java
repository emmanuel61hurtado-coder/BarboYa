package com.barboya.couriers;

import com.barboya.common.ApiException;
import com.barboya.common.Geo;
import com.barboya.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/courier/presence")
@PreAuthorize("hasRole('COURIER')")
public class PresenceController {

    public record PresenceRequest(
            @NotNull Boolean online,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude) {

        @Override
        public String toString() {
            return "PresenceRequest[online=" + online + "]";
        }
    }

    public record Presence(boolean online) {
    }

    private final CourierPresenceRepository presence;
    private final CourierService couriers;
    private final Clock clock;

    public PresenceController(CourierPresenceRepository presence, CourierService couriers, Clock clock) {
        this.presence = presence;
        this.couriers = couriers;
        this.clock = clock;
    }

    @GetMapping
    public Presence get(@AuthenticationPrincipal AuthUser user) {
        return new Presence(presence.findById(user.id()).map(CourierPresence::isOnline).orElse(false));
    }

    /**
     * Conectarse exige estar verificado. Al desconectarse se borra la posicion,
     * salvo que se siga enviando por una entrega en curso.
     */
    @PutMapping
    @Transactional
    public Presence update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PresenceRequest body) {
        Geo.Point position = body.latitude() == null || body.longitude() == null
                ? null : new Geo.Point(body.latitude(), body.longitude());
        if (body.online()) {
            couriers.requireOperable(user.id());
            if (position == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "LOCATION_REQUIRED",
                        "Comparte tu ubicacion para recibir pedidos");
            }
        }
        CourierPresence current = presence.findById(user.id()).orElse(null);
        if (current == null) {
            if (!body.online()) {
                return new Presence(false);
            }
            current = new CourierPresence(user.id());
        }
        current.update(body.online(), position, clock.instant());
        presence.save(current);
        return new Presence(body.online());
    }
}
