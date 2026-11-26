package com.barboya.security;

import com.barboya.common.ApiException;
import com.barboya.config.AppProperties;
import com.barboya.notifications.MailService;
import com.barboya.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityUnitTest {

    private static AppProperties properties(String secret) {
        return new AppProperties(
                new AppProperties.Security(secret, "i", "a", Duration.ofMinutes(15), Duration.ofDays(1),
                        Duration.ofSeconds(20), 5,
                        Duration.ofMinutes(15), true, 20, List.of()),
                null, new AppProperties.Mail(""), new AppProperties.BootstrapAdmin("", "", ""), null);
    }

    private static MockEnvironment environment(String profile) {
        MockEnvironment environment = new MockEnvironment();
        if (!profile.isEmpty()) {
            environment.setActiveProfiles(profile);
        }
        return environment;
    }

    @ParameterizedTest
    @CsvSource({
            "'', ''",
            "'', prod",
            "corto, prod",
            "corto, dev",
            "cambia-esto-por-un-secreto-largo-y-aleatorio, prod"})
    void refusesToStartWithoutAProperSecret(String secret, String profile) {
        assertThatThrownBy(() -> new SecretKeys(properties(secret), environment(profile)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void developmentGetsATemporaryRandomSecret() {
        SecretKeys first = new SecretKeys(properties(""), environment("dev"));
        SecretKeys second = new SecretKeys(properties(""), environment("dev"));

        assertThat(first.jwtKey().getEncoded()).isNotEqualTo(second.jwtKey().getEncoded());
    }

    @Test
    void aStrongSecretGivesStableKeys() {
        String secret = "x7Qm2".repeat(8);

        assertThat(new SecretKeys(properties(secret), environment("prod")).jwtKey().getEncoded())
                .isEqualTo(new SecretKeys(properties(secret), environment("prod")).jwtKey().getEncoded());
    }

    @Test
    void authenticationRequestsAreRateLimitedPerAddress() throws Exception {
        MutableClock clock = new MutableClock();
        var filter = new AuthRateLimitFilter(new RateLimiter(2, clock));

        assertThat(send(filter, "10.0.0.1")).isEqualTo(200);
        assertThat(send(filter, "10.0.0.1")).isEqualTo(200);
        assertThat(send(filter, "10.0.0.1")).isEqualTo(429);
        assertThat(send(filter, "10.0.0.2")).isEqualTo(200);

        clock.advance(Duration.ofMinutes(1));
        assertThat(send(filter, "10.0.0.1")).isEqualTo(200);
    }

    @Test
    void renewingASessionIsNotRateLimited() throws Exception {
        var filter = new AuthRateLimitFilter(new RateLimiter(1, new MutableClock()));

        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void withoutAMailServerCodesAreReportedUnavailable() {
        MailService mail = new MailService(mock(ObjectProvider.class), properties(""));

        assertThatThrownBy(mail::requireConfigured)
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("MAIL_NOT_CONFIGURED");
    }

    private static int send(AuthRateLimitFilter filter, String address) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(address);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
