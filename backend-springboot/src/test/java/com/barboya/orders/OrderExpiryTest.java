package com.barboya.orders;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pedidos sin respuesta y cierre por el equipo. */
class OrderExpiryTest extends CommerceFixtures {

    @Autowired private OrderExpiryService expiry;
    @Autowired private JdbcTemplate jdbc;

    private Account again(Account account) throws Exception {
        return account(login(account.email(), PASSWORD, "pruebas"));
    }

    private ResultActions read(Account customer, String id) throws Exception {
        return mvc.perform(get("/api/v1/orders/" + id).header("Authorization", customer.bearer()));
    }

    private int stock(UUID product) {
        return jdbc.queryForObject("select stock from products where id = ?", Integer.class, product);
    }

    @Test
    void anUnansweredOrderIsCancelledAndTheStockComesBack() throws Exception {
        Shop shop = openShop("Sin respuesta " + UUID.randomUUID());
        UUID product = addProduct(shop, "Empanada", 2000, 5);
        Account customer = register(Role.CUSTOMER);
        JsonNode order = placed(customer, orderData(shop, product, 2, "PICKUP", 4000));
        UUID id = UUID.fromString(order.get("id").asText());
        assertThat(order.get("respondBy").asText()).isNotBlank();
        assertThat(stock(product)).isEqualTo(3);

        clock.advance(Duration.ofMinutes(14));
        assertThat(expiry.expire(id)).isFalse();
        assertThat(expiry.overdue()).doesNotContain(id);

        clock.advance(Duration.ofMinutes(2));
        assertThat(expiry.overdue()).contains(id);
        assertThat(expiry.expire(id)).isTrue();
        assertThat(expiry.expire(id)).isFalse();

        assertThat(stock(product)).isEqualTo(5);
        read(again(customer), id.toString())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusReason").value("El comercio no respondio a tiempo"))
                .andExpect(jsonPath("$.respondBy").doesNotExist())
                .andExpect(jsonPath("$.history.length()").value(2));
        // El historial distingue un cambio del sistema de uno hecho por una persona
        assertThat(jdbc.queryForObject(
                "select count(*) from order_events where order_id = ? and actor_id is null", Integer.class, id))
                .isEqualTo(1);
        merchantAction(new Shop(again(shop.owner()), shop.businessId()), id.toString(), "accept",
                Map.of("prepMinutes", 10)).andExpect(status().isConflict());
    }

    @Test
    void anAnsweredOrderDoesNotExpire() throws Exception {
        Shop shop = openShop("Responde " + UUID.randomUUID());
        UUID product = addProduct(shop, "Arepa", 3000, null);
        Account customer = register(Role.CUSTOMER);
        String id = placed(customer, orderData(shop, product, 1, "PICKUP", 3000)).get("id").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 10)).andExpect(status().isOk());

        clock.advance(Duration.ofHours(2));
        assertThat(expiry.expire(UUID.fromString(id))).isFalse();
        read(again(customer), id).andExpect(jsonPath("$.status").value("PREPARING"));
    }

    @Test
    void staffCanFindAndCloseAnOrderLeavingATrace() throws Exception {
        Shop shop = openShop("Atascado " + UUID.randomUUID());
        UUID product = addProduct(shop, "Jugo", 4000, 8);
        Account customer = register(Role.CUSTOMER);
        JsonNode order = placed(customer, orderData(shop, product, 3, "PICKUP", 12000));
        String id = order.get("id").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5)).andExpect(status().isOk());
        Account admin = accountWithRole(Role.ADMIN);
        Map<String, String> reason = Map.of("reason", "El comercio cerro sin entregar");

        for (Account other : new Account[]{customer, shop.owner()}) {
            mvc.perform(get("/api/v1/admin/orders").header("Authorization", other.bearer()))
                    .andExpect(status().isForbidden());
            mvc.perform(postJson("/api/v1/admin/orders/" + id + "/close", reason)
                    .header("Authorization", other.bearer())).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/admin/orders?number=" + order.get("number").asLong())
                        .header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].customer.phone").value(customer.phone()))
                .andExpect(jsonPath("$.items[0].deliveryPin").doesNotExist());
        mvc.perform(postJson("/api/v1/admin/orders/" + id + "/close", Map.of("reason", ""))
                .header("Authorization", admin.bearer())).andExpect(status().isBadRequest());

        mvc.perform(postJson("/api/v1/admin/orders/" + id + "/close", reason)
                        .header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock(product)).isEqualTo(8);
        mvc.perform(postJson("/api/v1/admin/orders/" + id + "/close", reason)
                .header("Authorization", admin.bearer())).andExpect(status().isConflict());
        assertThat(stock(product)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from audit_events "
                + "where action = 'ORDER_CLOSED_BY_STAFF' and actor_id = ?", Integer.class, admin.id()))
                .isEqualTo(1);
    }
}
