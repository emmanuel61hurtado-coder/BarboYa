package com.barboya.couriers;

import com.barboya.common.Geo;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Disponibilidad y ultima posicion conocida. No se conserva el recorrido. */
@Entity
@Table(name = "courier_presence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierPresence {

    @Id
    private UUID courierId;
    private boolean online;
    private Double latitude;
    private Double longitude;
    private Instant seenAt;

    @Version
    private Long version;

    CourierPresence(UUID courierId) {
        this.courierId = courierId;
    }

    void update(boolean nowOnline, Geo.Point position, Instant now) {
        online = nowOnline;
        latitude = position == null ? null : position.latitude();
        longitude = position == null ? null : position.longitude();
        seenAt = position == null ? null : now;
    }

    /** La posicion solo vale si es reciente. */
    public Optional<Geo.Point> freshPosition(Instant now, Duration maxAge) {
        return seenAt == null || seenAt.plus(maxAge).isBefore(now)
                ? Optional.empty()
                : Optional.of(new Geo.Point(latitude, longitude));
    }
}
