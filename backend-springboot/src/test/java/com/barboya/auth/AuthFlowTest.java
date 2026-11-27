package com.barboya.auth;

import com.barboya.legal.LegalDocument;
import com.barboya.support.IntegrationTest;
import com.barboya.users.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends IntegrationTest {

    private Map<String, Object> registration(String email, String phone, String password, String role) {
        return Map.of("fullName", "Ana Prueba", "email", email, "phone", phone, "password", password, "role", role,
                "adult", true, "termsVersion", LegalDocument.TERMS.version(),
                "privacyVersion", LegalDocument.PRIVACY.version());
    }

    private MvcResult refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andReturn();
    }

    @Test
    void registrationOpensASessionWithoutExposingSecrets() throws Exception {
        MvcResult result = mvc.perform(postJson("/api/v1/auth/register",
                        registration(uniqueEmail(), uniquePhone(), PASSWORD, "CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(PASSWORD, "passwordHash", "argon2");
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("barboya_refresh=", "HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
        assertThat(statusOfMe(account(result))).isEqualTo(200);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "SUPERADMIN"})
    void publicRegistrationRejectsStaffRoles(Role role) throws Exception {
        mvc.perform(postJson("/api/v1/auth/register",
                        registration(uniqueEmail(), uniquePhone(), PASSWORD, role.name())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_ALLOWED"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"COURIER", "MOTO_DRIVER", "CAR_DRIVER", "RESTAURANT", "STORE"})
    void serviceProvidersStartPendingHumanReview(Role role) throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", register(role).bearer()))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @ParameterizedTest
    @CsvSource({
            "corta1,          3001234567, CUSTOMER",
            "sinNumerosLarga, 3001234567, CUSTOMER",
            "Segura12345,     abc,        CUSTOMER",
            "Segura12345,     3001234567, HACKER"})
    void invalidRegistrationIsRejected(String password, String phone, String role) throws Exception {
        mvc.perform(postJson("/api/v1/auth/register", registration(uniqueEmail(), phone, password, role)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameContactCannotRegisterTwice() throws Exception {
        var first = register(Role.CUSTOMER);

        mvc.perform(postJson("/api/v1/auth/register", registration(
                        first.email().toUpperCase(Locale.ROOT), uniquePhone(), PASSWORD, "CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        // El mismo celular escrito sin el prefijo del pais
        mvc.perform(postJson("/api/v1/auth/register", registration(
                        uniqueEmail(), first.phone().substring(3), PASSWORD, "CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_TAKEN"));
    }

    @Test
    void loginAcceptsEmailOrPhoneAndHidesWhichAccountsExist() throws Exception {
        var user = register(Role.CUSTOMER);

        assertThat(login(user.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(200);
        assertThat(login(user.phone().substring(3), PASSWORD, "a").getResponse().getStatus()).isEqualTo(200);

        var wrongPassword = login(user.email(), "Equivocada123", "a");
        var unknownAccount = login(uniqueEmail(), PASSWORD, "a");
        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknownAccount.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
    }

    @Test
    void repeatedFailuresLockTheAccountTemporarily() throws Exception {
        var user = register(Role.CUSTOMER);
        for (int i = 0; i < 5; i++) {
            login(user.email(), "Equivocada123", "a");
        }

        assertThat(login(user.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(401);

        clock.advance(Duration.ofMinutes(16));
        assertThat(login(user.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void refreshRotatesTheToken() throws Exception {
        var user = register(Role.CUSTOMER);

        MvcResult renewed = refresh(user.refresh());

        assertThat(renewed.getResponse().getStatus()).isEqualTo(200);
        assertThat(account(renewed).refresh().getValue()).isNotEqualTo(user.refresh().getValue());
        assertThat(statusOfMe(account(renewed))).isEqualTo(200);
    }

    private void assertSessionClosed(Account user, Account latest) throws Exception {
        assertThat(refresh(latest.refresh()).getResponse().getStatus()).isEqualTo(401);
        assertThat(statusOfMe(latest)).isEqualTo(401);
        mvc.perform(get("/api/v1/auth/history")
                        .header("Authorization", account(login(user.email(), PASSWORD, "a")).bearer()))
                .andExpect(jsonPath("$[?(@.action == 'REFRESH_TOKEN_REUSED')]").isNotEmpty());
    }

    @Test
    void reusingAnOldRefreshTokenClosesTheWholeSession() throws Exception {
        var user = register(Role.CUSTOMER);
        var renewed = account(refresh(user.refresh()));

        clock.advance(Duration.ofSeconds(21));
        assertThat(refresh(user.refresh()).getResponse().getStatus()).isEqualTo(401);

        assertSessionClosed(user, renewed);
    }

    /** Por ejemplo, recargar la pagina mientras se renovaba. */
    @Test
    void aLostRenewalResponseCanBeRetriedRightAway() throws Exception {
        var user = register(Role.CUSTOMER);
        refresh(user.refresh());

        MvcResult retried = refresh(user.refresh());
        MvcResult retriedAgain = refresh(user.refresh());

        assertThat(retried.getResponse().getStatus()).isEqualTo(200);
        assertThat(retriedAgain.getResponse().getStatus()).isEqualTo(200);
        var next = account(refresh(account(retriedAgain).refresh()));
        assertThat(statusOfMe(next)).isEqualTo(200);
    }

    @Test
    void theRetryWindowDoesNotApplyOnceTheNewTokenWasUsed() throws Exception {
        var user = register(Role.CUSTOMER);
        var second = account(refresh(user.refresh()));
        var third = account(refresh(second.refresh()));

        assertThat(refresh(user.refresh()).getResponse().getStatus()).isEqualTo(401);

        assertSessionClosed(user, third);
    }

    @Test
    void aTokenReplacedByARetryRevealsASecondCopy() throws Exception {
        var user = register(Role.CUSTOMER);
        var discarded = account(refresh(user.refresh()));
        var current = account(refresh(user.refresh()));

        assertThat(refresh(discarded.refresh()).getResponse().getStatus()).isEqualTo(401);

        assertSessionClosed(user, current);
    }

    @Test
    void refreshRequiresAKnownCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
        assertThat(refresh(new Cookie("barboya_refresh", "inventado")).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void logoutRevokesAccessAndRefresh() throws Exception {
        var user = register(Role.CUSTOMER);

        mvc.perform(post("/api/v1/auth/logout").cookie(user.refresh())).andExpect(status().isNoContent());

        assertThat(statusOfMe(user)).isEqualTo(401);
        assertThat(refresh(user.refresh()).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void accessTokenAndSessionExpire() throws Exception {
        var user = register(Role.CUSTOMER);

        clock.advance(Duration.ofMinutes(16));
        assertThat(statusOfMe(user)).isEqualTo(401);
        var renewed = account(refresh(user.refresh()));
        assertThat(statusOfMe(renewed)).isEqualTo(200);

        clock.advance(Duration.ofDays(31));
        assertThat(refresh(renewed.refresh()).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        var user = register(Role.CUSTOMER);
        String token = user.accessToken();
        int middle = token.lastIndexOf('.') + 5;
        char replacement = token.charAt(middle) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, middle) + replacement + token.substring(middle + 1);

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionsCanBeListedAndClosedRemotely() throws Exception {
        var phone = register(Role.CUSTOMER);
        var laptop = account(login(phone.email(), PASSWORD, "laptop"));
        var tablet = account(login(phone.email(), PASSWORD, "tablet"));

        MvcResult list = mvc.perform(get("/api/v1/auth/sessions").header("Authorization", phone.bearer()))
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();
        String laptopSession = null;
        for (var node : body(list)) {
            if ("laptop".equals(node.get("userAgent").asText())) {
                laptopSession = node.get("id").asText();
            }
        }

        mvc.perform(delete("/api/v1/auth/sessions/" + laptopSession).header("Authorization", phone.bearer()))
                .andExpect(status().isNoContent());
        assertThat(statusOfMe(laptop)).isEqualTo(401);
        assertThat(statusOfMe(tablet)).isEqualTo(200);

        mvc.perform(delete("/api/v1/auth/sessions").header("Authorization", phone.bearer()))
                .andExpect(status().isNoContent());
        assertThat(statusOfMe(tablet)).isEqualTo(401);
        assertThat(statusOfMe(phone)).isEqualTo(200);
    }

    @Test
    void nobodyCanCloseSomeoneElsesSession() throws Exception {
        var victim = register(Role.CUSTOMER);
        var attacker = register(Role.CUSTOMER);
        String victimSession = body(mvc.perform(get("/api/v1/auth/sessions")
                .header("Authorization", victim.bearer())).andReturn()).get(0).get("id").asText();

        mvc.perform(delete("/api/v1/auth/sessions/" + victimSession).header("Authorization", attacker.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/auth/sessions/" + UUID.randomUUID()).header("Authorization", attacker.bearer()))
                .andExpect(status().isNotFound());
        assertThat(statusOfMe(victim)).isEqualTo(200);
    }

    @Test
    void loginFromAnUnknownDeviceIsReported() throws Exception {
        var user = register(Role.CUSTOMER);

        login(user.email(), PASSWORD, "navegador-desconocido");

        assertThat(mailsTo(user.email()))
                .anyMatch(m -> m.getSubject().contains("Nuevo inicio de sesion"));
        mvc.perform(get("/api/v1/auth/history").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$[?(@.action == 'LOGIN_NEW_DEVICE')]").isNotEmpty());
    }
}
