package com.barboya.auth;

import com.barboya.support.IntegrationTest;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecoveryTest extends IntegrationTest {

    private static final String NEW_PASSWORD = "Renovada67890";

    private void forgot(String email) throws Exception {
        mvc.perform(postJson("/api/v1/auth/password/forgot", Map.of("email", email)))
                .andExpect(status().isAccepted());
    }

    private ResultActions reset(String email, String code) throws Exception {
        return mvc.perform(postJson("/api/v1/auth/password/reset",
                Map.of("email", email, "code", code, "newPassword", NEW_PASSWORD)));
    }

    private static String otherThan(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    void unknownEmailGetsTheSameAnswerAndNoMail() throws Exception {
        String email = uniqueEmail();

        forgot(email);

        assertThat(mailsTo(email)).isEmpty();
    }

    @Test
    void codeResetsThePasswordOnceAndClosesSessions() throws Exception {
        var user = register(Role.CUSTOMER);
        forgot(user.email());
        String code = lastCodeSentTo(user.email());

        reset(user.email(), otherThan(code)).andExpect(status().isBadRequest());
        reset(user.email(), code).andExpect(status().isNoContent());

        assertThat(statusOfMe(user)).isEqualTo(401);
        assertThat(login(user.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(401);
        assertThat(login(user.email(), NEW_PASSWORD, "a").getResponse().getStatus()).isEqualTo(200);
        reset(user.email(), code).andExpect(status().isBadRequest());
    }

    @Test
    void knowingTheAccountIsNotEnough() throws Exception {
        var user = register(Role.CUSTOMER);

        reset(user.email(), "123456").andExpect(status().isBadRequest());

        assertThat(login(user.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void codeStopsWorkingAfterTooManyAttempts() throws Exception {
        var user = register(Role.CUSTOMER);
        forgot(user.email());
        String code = lastCodeSentTo(user.email());

        for (int i = 0; i < 5; i++) {
            reset(user.email(), otherThan(code)).andExpect(status().isBadRequest());
        }

        reset(user.email(), code).andExpect(status().isBadRequest());
    }

    @Test
    void codeExpires() throws Exception {
        var user = register(Role.CUSTOMER);
        forgot(user.email());
        String code = lastCodeSentTo(user.email());

        clock.advance(Duration.ofMinutes(11));

        reset(user.email(), code).andExpect(status().isBadRequest());
    }

    @Test
    void resendingIsLimitedAndOnlyTheLatestCodeWorks() throws Exception {
        var user = register(Role.CUSTOMER);
        forgot(user.email());
        String first = lastCodeSentTo(user.email());
        forgot(user.email());
        assertThat(mailsTo(user.email())).hasSize(1);

        clock.advance(Duration.ofSeconds(61));
        forgot(user.email());
        assertThat(mailsTo(user.email())).hasSize(2);
        String second = lastCodeSentTo(user.email());
        if (!first.equals(second)) {
            reset(user.email(), first).andExpect(status().isBadRequest());
        }
        reset(user.email(), second).andExpect(status().isNoContent());
    }

    @Test
    void dailyLimitStopsFurtherCodes() throws Exception {
        var user = register(Role.CUSTOMER);

        for (int i = 0; i < 7; i++) {
            forgot(user.email());
            clock.advance(Duration.ofSeconds(61));
        }

        assertThat(mailsTo(user.email())).hasSize(5);
    }

    @Test
    void emailIsVerifiedWithTheMailedCode() throws Exception {
        var user = register(Role.CUSTOMER);

        mvc.perform(post("/api/v1/auth/email/verification").header("Authorization", user.bearer()))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/auth/email/verification").header("Authorization", user.bearer()))
                .andExpect(status().isTooManyRequests());
        String code = lastCodeSentTo(user.email());

        mvc.perform(postJson("/api/v1/auth/email/verify", Map.of("code", otherThan(code)))
                        .header("Authorization", user.bearer()))
                .andExpect(status().isBadRequest());
        mvc.perform(postJson("/api/v1/auth/email/verify", Map.of("code", code))
                        .header("Authorization", user.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/users/me").header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void aPasswordResetCodeCannotVerifyEmail() throws Exception {
        var user = register(Role.CUSTOMER);
        forgot(user.email());
        String code = lastCodeSentTo(user.email());

        mvc.perform(postJson("/api/v1/auth/email/verify", Map.of("code", code))
                        .header("Authorization", user.bearer()))
                .andExpect(status().isBadRequest());
    }
}
