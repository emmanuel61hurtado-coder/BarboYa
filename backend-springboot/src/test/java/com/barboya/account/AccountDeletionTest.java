package com.barboya.account;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountDeletionTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private ResultActions delete(Account who, String password) throws Exception {
        return mvc.perform(postJson("/api/v1/account/delete", Map.of("password", password))
                .header("Authorization", who.bearer()));
    }

    private String column(String name, UUID id) {
        return jdbc.queryForObject("select " + name + " from users where id = ?", String.class, id);
    }

    @Test
    void thePersonalDataDisappearsAndTheHistoryRemains() throws Exception {
        Shop shop = openShop("Historia " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bunuelo", 1500, null);
        Account customer = register(Role.CUSTOMER);
        String order = placed(customer, orderData(shop, product, 1, "PICKUP", 1500)).get("id").asText();
        mvc.perform(postJson("/api/v1/addresses", Map.of("label", "Casa", "address", "Calle 1 # 2-3"))
                .header("Authorization", customer.bearer())).andExpect(status().isCreated());

        delete(customer, "Equivocada123").andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        delete(customer, PASSWORD).andExpect(jsonPath("$.code").value("ACTIVE_ORDERS"));
        mvc.perform(postJson("/api/v1/orders/" + order + "/cancel", Map.of())
                .header("Authorization", customer.bearer())).andExpect(status().isOk());
        delete(customer, PASSWORD).andExpect(status().isNoContent());

        assertThat(statusOfMe(customer)).isEqualTo(401);
        assertThat(login(customer.email(), PASSWORD, "a").getResponse().getStatus()).isEqualTo(401);
        assertThat(column("full_name", customer.id())).isEqualTo("Cuenta eliminada");
        assertThat(column("email", customer.id())).doesNotContain(customer.email()).endsWith("@eliminada.invalid");
        assertThat(column("phone", customer.id())).doesNotContain(customer.phone().substring(3));
        assertThat(jdbc.queryForObject("select count(*) from customer_addresses where user_id = ?",
                Integer.class, customer.id())).isZero();
        // El pedido y las autorizaciones siguen como prueba
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id = ?", Integer.class,
                customer.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from legal_acceptances where user_id = ?",
                Integer.class, customer.id())).isEqualTo(3);

        // El correo y el celular quedan libres y nadie puede revivir la cuenta
        mvc.perform(postJson("/api/v1/auth/register", Map.of(
                "fullName", "Otra Persona", "email", customer.email(), "phone", customer.phone(),
                "password", PASSWORD, "adult", true,
                "termsVersion", com.barboya.legal.LegalDocument.TERMS.version(),
                "privacyVersion", com.barboya.legal.LegalDocument.PRIVACY.version())))
                .andExpect(status().isCreated());
        Account admin = accountWithRole(Role.ADMIN);
        mvc.perform(patch("/api/v1/admin/users/" + customer.id() + "/status")
                        .header("Authorization", admin.bearer()).contentType("application/json")
                        .content("{\"status\":\"ACTIVE\",\"reason\":\"Reactivar\"}"))
                .andExpect(jsonPath("$.code").value("ACCOUNT_DELETED"));
    }

    @Test
    void aShopWithDebtCannotLeaveAndOnceGoneIsNotListed() throws Exception {
        Shop shop = openShop("Se va " + UUID.randomUUID());
        UUID product = addProduct(shop, "Pandebono", 2000, null);
        Account customer = register(Role.CUSTOMER);
        var order = placed(customer, orderData(shop, product, 1, "PICKUP", 2000));
        String id = order.get("id").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5));
        delete(shop.owner(), PASSWORD).andExpect(jsonPath("$.code").value("ACTIVE_ORDERS"));
        merchantAction(shop, id, "ready", Map.of());
        merchantAction(shop, id, "deliver", Map.of("pin", order.get("deliveryPin").asText()))
                .andExpect(status().isOk());

        delete(shop.owner(), PASSWORD).andExpect(jsonPath("$.code").value("PENDING_DEBT"));

        Shop clean = openShop("Sin deuda " + UUID.randomUUID());
        delete(clean.owner(), PASSWORD).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/catalog/businesses/" + clean.businessId())
                .header("Authorization", customer.bearer())).andExpect(status().isNotFound());
    }

    @Test
    void staffAccountsAreNotSelfDeleted() throws Exception {
        delete(accountWithRole(Role.ADMIN), PASSWORD).andExpect(jsonPath("$.code").value("STAFF_ACCOUNT"));
    }
}
