package com.barboya.ratings;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RatingTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private String order(Shop shop, Account customer) throws Exception {
        UUID product = addProduct(shop, "Plato " + UUID.randomUUID(), 8000, null);
        return placed(customer, orderData(shop, product, 1, "PICKUP", 8000)).get("id").asText();
    }

    private String deliveredOrder(Shop shop, Account customer) throws Exception {
        JsonNode order = placedOrder(shop, customer);
        String id = order.get("id").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5)).andExpect(status().isOk());
        merchantAction(shop, id, "ready", Map.of()).andExpect(status().isOk());
        merchantAction(shop, id, "deliver", Map.of("pin", order.get("deliveryPin").asText()))
                .andExpect(status().isOk());
        return id;
    }

    private JsonNode placedOrder(Shop shop, Account customer) throws Exception {
        UUID product = addProduct(shop, "Plato " + UUID.randomUUID(), 8000, null);
        return placed(customer, orderData(shop, product, 1, "PICKUP", 8000));
    }

    private ResultActions rate(Account who, String order, String target, int stars, String comment)
            throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("target", target);
        data.put("stars", stars);
        data.put("comment", comment);
        return mvc.perform(postJson("/api/v1/orders/" + order + "/ratings", data)
                .header("Authorization", who.bearer()));
    }

    private ResultActions read(Account who, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", who.bearer()));
    }

    private ResultActions send(Account who, String url, Map<String, ?> data) throws Exception {
        return mvc.perform(postJson(url, data).header("Authorization", who.bearer()));
    }

    @Test
    void onlyTheCustomerOfADeliveredOrderCanRateItOnce() throws Exception {
        Shop shop = openShop("Calificado " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        String pending = order(shop, customer);
        rate(customer, pending, "BUSINESS", 5, null).andExpect(jsonPath("$.code").value("NOT_DELIVERED"));
        read(customer, "/api/v1/orders/" + pending + "/ratings").andExpect(jsonPath("$.openUntil").doesNotExist());

        String id = deliveredOrder(shop, customer);
        read(customer, "/api/v1/orders/" + id + "/ratings")
                .andExpect(jsonPath("$.openUntil").exists())
                .andExpect(jsonPath("$.targets.length()").value(1))
                .andExpect(jsonPath("$.targets[0].target").value("BUSINESS"))
                .andExpect(jsonPath("$.targets[0].summary.count").value(0));
        rate(register(Role.CUSTOMER), id, "BUSINESS", 1, null).andExpect(status().isNotFound());
        rate(shop.owner(), id, "BUSINESS", 5, null).andExpect(status().isForbidden());
        rate(customer, id, "BUSINESS", 0, null).andExpect(status().isBadRequest());
        rate(customer, id, "BUSINESS", 6, null).andExpect(status().isBadRequest());
        rate(customer, id, "BUSINESS", 5, "x".repeat(501)).andExpect(status().isBadRequest());
        rate(customer, id, "COURIER", 5, null).andExpect(jsonPath("$.code").value("NOTHING_TO_RATE"));

        rate(customer, id, "BUSINESS", 4, "  Muy rico  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targets[0].mine.stars").value(4))
                .andExpect(jsonPath("$.targets[0].mine.comment").value("Muy rico"))
                .andExpect(jsonPath("$.targets[0].summary.average").value(4.0));
        rate(customer, id, "BUSINESS", 1, null).andExpect(jsonPath("$.code").value("ALREADY_RATED"));
    }

    @Test
    void theTimeToRateRunsOut() throws Exception {
        Shop shop = openShop("Tarde " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        String id = deliveredOrder(shop, customer);

        clock.advance(Duration.ofDays(8));
        Account later = account(login(customer.email(), PASSWORD, "pruebas"));
        read(later, "/api/v1/orders/" + id + "/ratings").andExpect(jsonPath("$.openUntil").doesNotExist());
        rate(later, id, "BUSINESS", 5, null).andExpect(jsonPath("$.code").value("RATING_CLOSED"));
    }

    @Test
    void averagesAndReviewsNeverRevealWhoWroteThem() throws Exception {
        Shop shop = openShop("Opiniones " + UUID.randomUUID());
        Account first = register(Role.CUSTOMER);
        Account second = register(Role.CUSTOMER);
        rate(first, deliveredOrder(shop, first), "BUSINESS", 5, "Excelente").andExpect(status().isCreated());
        rate(second, deliveredOrder(shop, second), "BUSINESS", 2, " ").andExpect(status().isCreated());

        read(first, "/api/v1/ratings/businesses?ids=" + shop.businessId())
                .andExpect(jsonPath("$['" + shop.businessId() + "'].average").value(3.5))
                .andExpect(jsonPath("$['" + shop.businessId() + "'].count").value(2));
        String reviews = read(second, "/api/v1/ratings/businesses/" + shop.businessId())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].comment").value("Excelente"))
                .andReturn().getResponse().getContentAsString();
        assertThat(reviews).doesNotContain(first.id().toString(), "orderId", "authorName");
        read(shop.owner(), "/api/v1/ratings/me")
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.summary.average").value(3.5));
        read(first, "/api/v1/ratings/me").andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/ratings/businesses/" + shop.businessId())).andExpect(status().isUnauthorized());
    }

    @Test
    void staffCanHideAnAbusiveRatingWithAReason() throws Exception {
        Shop shop = openShop("Moderado " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        String id = deliveredOrder(shop, customer);
        rate(customer, id, "BUSINESS", 1, "Insulto de prueba").andExpect(status().isCreated());
        Account admin = accountWithRole(Role.ADMIN);
        String ratingId = jdbc.queryForObject("select id from ratings where order_id = ?", String.class,
                UUID.fromString(id));
        String base = "/api/v1/admin/ratings/" + ratingId;
        Map<String, String> reason = Map.of("reason", "Contiene insultos hacia el personal");

        read(shop.owner(), "/api/v1/admin/ratings").andExpect(status().isForbidden());
        send(shop.owner(), base + "/hide", reason).andExpect(status().isForbidden());
        send(admin, base + "/hide", Map.of("reason", "corto")).andExpect(status().isBadRequest());
        send(admin, base + "/hide", reason)
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.targetName").isNotEmpty())
                .andExpect(jsonPath("$.authorName").isNotEmpty());
        send(admin, base + "/hide", reason).andExpect(jsonPath("$.code").value("NO_CHANGE"));
        read(shop.owner(), "/api/v1/ratings/me")
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.summary.count").value(0))
                .andExpect(jsonPath("$.summary.average").doesNotExist());
        // Ocultar no permite volver a calificar
        rate(customer, id, "BUSINESS", 5, null).andExpect(jsonPath("$.code").value("ALREADY_RATED"));

        send(admin, base + "/restore", Map.of("reason", "Revisado, no incumple las normas"))
                .andExpect(jsonPath("$.hidden").value(false));
        read(shop.owner(), "/api/v1/ratings/me").andExpect(jsonPath("$.total").value(1));
        assertThat(jdbc.queryForObject("select count(*) from audit_events where actor_id = ? "
                + "and action in ('RATING_HIDDEN', 'RATING_RESTORED')", Integer.class, admin.id())).isEqualTo(2);

        for (String statement : new String[]{
                "update ratings set stars = 5", "update ratings set comment = 'otro'",
                "delete from ratings", "truncate ratings"}) {
            assertThatThrownBy(() -> jdbc.execute(statement)).isInstanceOf(DataAccessException.class);
        }
    }
}
