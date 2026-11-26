package com.barboya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "barboya")
public record AppProperties(
        Security security, Codes codes, Mail mail, BootstrapAdmin bootstrapAdmin, Files files) {

    public record Files(@DefaultValue("./data/files") String directory, @DefaultValue("") String key) {
    }

    public record Security(
            @DefaultValue("") String jwtSecret,
            String issuer,
            String audience,
            Duration accessTokenTtl,
            Duration sessionTtl,
            @DefaultValue("20s") Duration refreshRetryWindow,
            int maxFailedLogins,
            Duration lockDuration,
            @DefaultValue("true") boolean cookieSecure,
            int authRequestsPerMinute,
            @DefaultValue List<String> corsAllowedOrigins) {
    }

    public record Codes(Duration ttl, int maxAttempts, Duration resendCooldown, int maxPerDay) {
    }

    public record Mail(@DefaultValue("") String from) {
    }

    public record BootstrapAdmin(
            @DefaultValue("") String email,
            @DefaultValue("") String phone,
            @DefaultValue("") String password) {
    }
}
