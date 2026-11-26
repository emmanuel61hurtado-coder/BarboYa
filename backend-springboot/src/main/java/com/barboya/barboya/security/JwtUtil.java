package com.barboya.barboya.security;

import com.barboya.barboya.config.BarboyaProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final BarboyaProperties props;

    public JwtUtil(BarboyaProperties props) {
        this.props = props;
    }

    private SecretKey key() {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hash);
        } catch (Exception e) {
            return Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        }
    }

    public String createToken(UUID userId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getJwt().getExpireMinutes() * 60L);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuer(props.getJwt().getIssuer())
                .audience().add(props.getJwt().getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key())
                .compact();
    }

    public Claims decode(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireIssuer(props.getJwt().getIssuer())
                .requireAudience(props.getJwt().getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}