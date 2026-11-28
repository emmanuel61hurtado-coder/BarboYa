package com.barboya.orders;

import com.barboya.common.BusinessDays;
import com.barboya.common.Colombia;
import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Plazo de entrega prometido y fechas limite de los reclamos. */
class DeliveryDeadlineTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private ResultActions cancel(Account customer, String id) throws Exception {
        return mvc.perform(postJson("/api/v1/orders/" + id + "/cancel", Map.of())
                .header("Authorization", customer.bearer()));
    }

    @Test
    void anAcceptedOrderCanBeCancelledOnlyAfterThePromisedTime() throws Exception {
        Shop shop = openShop("Plazo " + UUID.randomUUID());
        UUID product = addProduct(shop, "Pizza", 20000, 3);
        Account customer = register(Role.CUSTOMER);
        JsonNode order = placed(customer, orderData(shop, product, 2, "PICKUP", 40000));
        String id = order.get("id").asText();
        assertThat(Duration.between(Instant.parse(order.get("createdAt").asText()),
                Instant.parse(order.get("deliverBy").asText()))).isEqualTo(Duration.ofMinutes(60));
        assertThat(order.get("cancellable").asBoolean()).isTrue();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 10));

        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.cancellable").value(false));
        cancel(customer, id).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));

        clock.advance(Duration.ofMinutes(61));
        Account later = account(login(customer.email(), PASSWORD, "pruebas"));
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", later.bearer()))
                .andExpect(jsonPath("$.cancellable").value(true));
        cancel(later, id).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusReason")
                        .value("El cliente cancelo porque se vencio el plazo de entrega"))
                .andExpect(jsonPath("$.cancellable").value(false));
        assertThat(jdbc.queryForObject("select stock from products where id = ?", Integer.class, product))
                .isEqualTo(3);
    }

    @Test
    void aShopWithoutADeliveryTimeCannotOpen() throws Exception {
        Account owner = register(Role.RESTAURANT);
        approve(owner);
        var data = businessData("Sin plazo");
        data.put("maxDeliveryMinutes", 5);
        saveBusiness(owner, data).andExpect(status().isBadRequest());
        data.remove("maxDeliveryMinutes");
        saveBusiness(owner, data).andExpect(status().isBadRequest());
    }

    @Test
    void businessDaysSkipWeekends() {
        // El 2026-11-27 es viernes: 1 dia habil despues es el lunes 30
        assertThat(LocalDate.ofInstant(BusinessDays.deadline(LocalDate.of(2026, 11, 27), 1), Colombia.ZONE))
                .isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(LocalDate.ofInstant(BusinessDays.deadline(LocalDate.of(2026, 11, 28), 10), Colombia.ZONE))
                .isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(LocalDate.ofInstant(BusinessDays.deadline(LocalDate.of(2026, 11, 27), 15), Colombia.ZONE))
                .isEqualTo(LocalDate.of(2026, 12, 18));
    }

    @Test
    void eachClaimHasItsLegalDeadlineAndTheMostUrgentComesFirst() throws Exception {
        Account customer = register(Role.CUSTOMER);
        Account admin = accountWithRole(Role.ADMIN);
        LocalDate today = Colombia.today(clock);
        for (String category : new String[]{"OTHER", "PERSONAL_DATA"}) {
            JsonNode ticket = body(mvc.perform(postJson("/api/v1/support/tickets", Map.of(
                            "category", category, "subject", "Consulta de plazo",
                            "message", "Quiero saber cuando responden"))
                    .header("Authorization", customer.bearer())).andExpect(status().isCreated()).andReturn())
                    .get("ticket");
            assertThat(Instant.parse(ticket.get("dueAt").asText())).isEqualTo(
                    BusinessDays.deadline(today, category.equals("PERSONAL_DATA") ? 10 : 15));
        }

        JsonNode queue = body(mvc.perform(get("/api/v1/admin/support/tickets")
                .header("Authorization", admin.bearer())).andReturn()).get("items");
        for (int i = 1; i < queue.size(); i++) {
            assertThat(Instant.parse(queue.get(i).get("dueAt").asText()))
                    .isAfterOrEqualTo(Instant.parse(queue.get(i - 1).get("dueAt").asText()));
        }
    }
}
