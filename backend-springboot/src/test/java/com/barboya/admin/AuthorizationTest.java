package com.barboya.admin;

import com.barboya.support.IntegrationTest;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthorizationTest extends IntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationContext context;

    private ResultActions changeStatus(Account actor, UUID target, String newStatus) throws Exception {
        return mvc.perform(patch("/api/v1/admin/users/" + target + "/status")
                .header("Authorization", actor.bearer())
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("status", newStatus, "reason", "Revision documental"))));
    }

    private ResultActions createAdmin(Account actor) throws Exception {
        return mvc.perform(postJson("/api/v1/admin/staff", Map.of(
                        "fullName", "Nueva Persona", "email", uniqueEmail(), "phone", uniquePhone(),
                        "password", "ClaveInterna2026"))
                .header("Authorization", actor.bearer()));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void onlyStaffReachesAdministration(Role role) throws Exception {
        var actor = accountWithRole(role);
        int expected = role.isStaff() ? 200 : 403;

        for (String path : new String[]{"/api/v1/admin/users", "/api/v1/admin/audit"}) {
            mvc.perform(get(path).header("Authorization", actor.bearer()))
                    .andExpect(status().is(expected));
        }
        assertThat(statusOfMe(actor)).isEqualTo(200);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void onlyTheSuperadminCreatesAdmins(Role role) throws Exception {
        createAdmin(accountWithRole(role))
                .andExpect(status().is(role == Role.SUPERADMIN ? 201 : 403));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/users/me", "/api/v1/auth/sessions", "/api/v1/auth/history",
            "/api/v1/admin/users", "/api/v1/admin/audit"})
    void anonymousRequestsAreRejected(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test
    void approvalAfterReviewIsAudited() throws Exception {
        var admin = accountWithRole(Role.ADMIN);
        var courier = register(Role.COURIER);

        changeStatus(admin, courier.id(), "ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(get("/api/v1/admin/audit?size=100").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.items[?(@.targetId == '" + courier.id()
                        + "' && @.action == 'USER_STATUS_CHANGED')].detail")
                        .value("PENDING_VERIFICATION -> ACTIVE: Revision documental"));
    }

    @Test
    void usersCanBeFilteredByStatus() throws Exception {
        var admin = accountWithRole(Role.ADMIN);
        register(Role.COURIER);

        mvc.perform(get("/api/v1/admin/users?status=PENDING_VERIFICATION&size=100")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.status != 'PENDING_VERIFICATION')]").isEmpty());
        mvc.perform(get("/api/v1/admin/users?status=OTRO").header("Authorization", admin.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suspensionTakesEffectImmediately() throws Exception {
        var admin = accountWithRole(Role.ADMIN);
        var customer = register(Role.CUSTOMER);

        changeStatus(admin, customer.id(), "SUSPENDED").andExpect(status().isOk());

        assertThat(statusOfMe(customer)).isEqualTo(401);
        mvc.perform(post("/api/v1/auth/refresh").cookie(customer.refresh())).andExpect(status().isUnauthorized());
        assertThat(login(customer.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void adminsCannotTouchInternalAccountsOrThemselves() throws Exception {
        var admin = accountWithRole(Role.ADMIN);
        var colleague = accountWithRole(Role.ADMIN);
        var boss = accountWithRole(Role.SUPERADMIN);

        changeStatus(admin, boss.id(), "SUSPENDED").andExpect(status().isForbidden());
        changeStatus(admin, colleague.id(), "SUSPENDED").andExpect(status().isForbidden());
        changeStatus(admin, admin.id(), "SUSPENDED").andExpect(status().isConflict());
        changeStatus(boss, colleague.id(), "SUSPENDED").andExpect(status().isOk());
        assertThat(statusOfMe(colleague)).isEqualTo(401);
    }

    @Test
    void customersCannotApproveAccounts() throws Exception {
        var courier = register(Role.COURIER);

        changeStatus(courier, courier.id(), "ACTIVE").andExpect(status().isForbidden());
    }

    /** Rutas del esqueleto anterior, que no comprobaban permisos. */
    @ParameterizedTest
    @ValueSource(strings = {
            "/auth/register", "/trips", "/payments/crypto", "/wallet/topup", "/admin/drivers",
            "/admin/blockchain/sync", "/admin/subscriptions", "/merchants", "/orders",
            "/security/sos", "/webhooks/payments", "/public/trips/share/x", "/ws"})
    void legacyRoutesAreClosed(String path) throws Exception {
        var boss = accountWithRole(Role.SUPERADMIN);

        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", boss.bearer())).andExpect(status().isForbidden());
    }

    @Test
    void noLegacyComponentIsLoaded() {
        assertThat(Arrays.stream(context.getBeanDefinitionNames())
                .map(context::getType)
                .filter(type -> type != null && type.getName().startsWith("com.barboya.barboya.")))
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "update audit_events set detail = 'alterado'",
            "delete from audit_events",
            "truncate audit_events"})
    void auditHistoryCannotBeRewritten(String statement) throws Exception {
        register(Role.CUSTOMER);

        assertThatThrownBy(() -> jdbc.execute(statement))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("solo insercion");
    }

    @Test
    void healthIsPublicAndRevealsNoDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
