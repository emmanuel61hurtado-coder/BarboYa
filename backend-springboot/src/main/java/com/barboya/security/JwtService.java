package com.barboya.security;

import com.barboya.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Token de acceso de corta duracion, atado a una sesion que se puede revocar. */
@Service
public class JwtService {

    public record AccessClaims(UUID userId, UUID sessionId) {
    }

    private final AppProperties.Security settings;
    private final SecretKeys keys;
    private final Clock clock;

    public JwtService(AppProperties properties, SecretKeys keys, Clock clock) {
        this.settings = properties.security();
        this.keys = keys;
        this.clock = clock;
    }

    public String create(UUID userId, UUID sessionId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("sid", sessionId.toString())
                .issuer(settings.issuer())
                .audience().add(settings.audience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(settings.accessTokenTtl())))
                .signWith(keys.jwtKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Optional<AccessClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keys.jwtKey())
                    .requireIssuer(settings.issuer())
                    .requireAudience(settings.audience())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AccessClaims(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get("sid", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
