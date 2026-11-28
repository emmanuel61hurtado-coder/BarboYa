package com.barboya.support;

import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportTest extends CommerceFixtures {

    private static final String BASE = "/api/v1/support/tickets";
    private static final String STAFF = "/api/v1/admin/support/tickets";

    @Autowired private JdbcTemplate jdbc;

    private ResultActions open(Account who, String orderId) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("category", orderId == null ? "ACCOUNT" : "ORDER_PROBLEM");
        data.put("subject", "No llego completo");
        data.put("message", "Faltaba una bebida en el pedido");
        data.put("orderId", orderId);
        return mvc.perform(postJson(BASE, data).header("Authorization", who.bearer()));
    }

    private String opened(Account who, String orderId) throws Exception {
        return body(open(who, orderId).andExpect(status().isCreated()).andReturn())
                .get("ticket").get("id").asText();
    }

    private ResultActions send(Account who, String url, Map<String, ?> data) throws Exception {
        return mvc.perform(postJson(url, data).header("Authorization", who.bearer()));
    }

    private ResultActions read(Account who, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", who.bearer()));
    }

    private String order(Shop shop, Account customer) throws Exception {
        UUID product = addProduct(shop, "Combo " + UUID.randomUUID(), 9000, null);
        return placed(customer, orderData(shop, product, 1, "PICKUP", 9000)).get("id").asText();
    }

    @Test
    void aClaimIsAnsweredResolvedAndCanBeReopened() throws Exception {
        Shop shop = openShop("Reclamos " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        Account admin = accountWithRole(Role.ADMIN);
        String orderId = order(shop, customer);
        String id = opened(customer, orderId);

        open(customer, orderId).andExpect(jsonPath("$.code").value("TICKET_EXISTS"));
        JsonNode queue = body(read(admin, STAFF).andReturn());
        assertThat(queue.get("items").findValuesAsText("id")).contains(id);
        read(admin, STAFF + "/" + id)
                .andExpect(jsonPath("$.opener.phone").value(customer.phone()))
                .andExpect(jsonPath("$.ticket.orderId").value(orderId));

        send(admin, STAFF + "/" + id + "/messages", Map.of("body", "Vamos a revisar con el comercio"))
                .andExpect(jsonPath("$.ticket.status").value("ANSWERED"));
        assertThat(mailsTo(customer.email()))
                .anyMatch(mail -> mail.getSubject().equals("Respuesta de soporte BarboYa")
                        && !mail.getText().contains("revisar con el comercio"));
        read(customer, BASE + "/" + id)
                .andExpect(jsonPath("$.opener").doesNotExist())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[1].staff").value(true));
        send(customer, BASE + "/" + id + "/messages", Map.of("body", "Gracias, quedo atento"))
                .andExpect(jsonPath("$.ticket.status").value("OPEN"));

        Map<String, String> resolution = Map.of("resolution", "El comercio repuso la bebida");
        send(customer, STAFF + "/" + id + "/resolve", resolution).andExpect(status().isForbidden());
        send(admin, STAFF + "/" + id + "/resolve", resolution)
                .andExpect(jsonPath("$.ticket.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("El comercio repuso la bebida"));
        send(admin, STAFF + "/" + id + "/resolve", resolution)
                .andExpect(jsonPath("$.code").value("ALREADY_RESOLVED"));
        assertThat(jdbc.queryForObject("select count(*) from audit_events "
                + "where action = 'TICKET_RESOLVED' and actor_id = ?", Integer.class, admin.id())).isEqualTo(1);

        send(customer, BASE + "/" + id + "/messages", Map.of("body", "No me la entregaron"))
                .andExpect(jsonPath("$.ticket.status").value("OPEN"))
                .andExpect(jsonPath("$.resolution").doesNotExist())
                .andExpect(jsonPath("$.messages.length()").value(5));
    }

    @Test
    void onlyParticipantsOpenClaimsAndOnlyTheirAuthorReadsThem() throws Exception {
        Shop shop = openShop("Privado " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        Account stranger = register(Role.CUSTOMER);
        String orderId = order(shop, customer);
        String id = opened(customer, orderId);

        open(stranger, orderId).andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        read(stranger, BASE + "/" + id).andExpect(status().isNotFound());
        send(stranger, BASE + "/" + id + "/messages", Map.of("body", "Hola"))
                .andExpect(status().isNotFound());
        read(stranger, BASE).andExpect(jsonPath("$.total").value(0));
        read(customer, STAFF).andExpect(status().isForbidden());
        read(customer, STAFF + "/" + id).andExpect(status().isForbidden());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        // El comercio tambien participa, y su caso es independiente
        String fromShop = opened(shop.owner(), orderId);
        read(customer, BASE + "/" + fromShop).andExpect(status().isNotFound());
        read(shop.owner(), BASE).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void reopeningCannotDuplicateAnOpenClaim() throws Exception {
        Shop shop = openShop("Duplicado " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        Account admin = accountWithRole(Role.ADMIN);
        String orderId = order(shop, customer);
        String old = opened(customer, orderId);
        send(admin, STAFF + "/" + old + "/resolve", Map.of("resolution", "Quedo solucionado"))
                .andExpect(status().isOk());
        opened(customer, orderId);

        send(customer, BASE + "/" + old + "/messages", Map.of("body", "Sigue igual"))
                .andExpect(jsonPath("$.code").value("TICKET_EXISTS"));
    }

    @Test
    void thereIsALimitOfOpenClaimsAndTheTextIsValidated() throws Exception {
        Account customer = register(Role.CUSTOMER);
        send(customer, BASE, Map.of("category", "OTHER", "subject", "Hey", "message", "corto"))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 10; i++) {
            opened(customer, null);
        }
        open(customer, null).andExpect(jsonPath("$.code").value("TOO_MANY_TICKETS"));
    }

    @Test
    void messagesCannotBeRewritten() throws Exception {
        opened(register(Role.CUSTOMER), null);

        for (String statement : new String[]{
                "update support_messages set body = 'alterado'", "delete from support_messages",
                "truncate support_messages"}) {
            assertThatThrownBy(() -> jdbc.execute(statement)).isInstanceOf(DataAccessException.class);
        }
    }
}
