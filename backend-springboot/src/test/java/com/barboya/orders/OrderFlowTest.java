package com.barboya.orders;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderFlowTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private int stockOf(UUID product) {
        return jdbc.queryForObject("select stock from products where id = ?", Integer.class, product);
    }

    private static String otherThan(String pin) {
        return pin.equals("0000") ? "1111" : "0000";
    }

    @Test
    void aDeliveryGoesFromOrderToPinConfirmation() throws Exception {
        Shop shop = openShop("La Fogata");
        UUID product = addProduct(shop, "Arepa rellena", 8000, 10);
        Account customer = register(Role.CUSTOMER);

        JsonNode order = placed(customer, orderData(shop, product, 2, "DELIVERY", 19000));
        String id = order.get("id").asText();
        String pin = order.get("deliveryPin").asText();
        assertThat(pin).matches("\\d{4}");
        assertThat(order.get("status").asText()).isEqualTo("PLACED");
        assertThat(order.get("subtotalCop").asInt()).isEqualTo(16000);
        assertThat(order.get("deliveryFeeCop").asInt()).isEqualTo(3000);
        assertThat(stockOf(product)).isEqualTo(8);

        // El comercio ve a quien entregar, pero no el PIN
        mvc.perform(get("/api/v1/merchant/orders").header("Authorization", shop.owner().bearer()))
                .andExpect(jsonPath("$.items[0].customer.phone").value(customer.phone()))
                .andExpect(jsonPath("$.items[0].deliveryPin").doesNotExist());

        // No se puede saltar pasos
        merchantAction(shop, id, "deliver", Map.of("pin", pin)).andExpect(status().isConflict());
        merchantAction(shop, id, "ready", Map.of()).andExpect(status().isConflict());

        merchantAction(shop, id, "accept", Map.of("prepMinutes", 25)).andExpect(jsonPath("$.status").value("PREPARING"));
        merchantAction(shop, id, "ready", Map.of()).andExpect(jsonPath("$.status").value("READY"));
        merchantAction(shop, id, "deliver", Map.of("pin", pin)).andExpect(status().isConflict());
        merchantAction(shop, id, "dispatch", Map.of()).andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));
        merchantAction(shop, id, "deliver", Map.of("pin", otherThan(pin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PIN"));
        merchantAction(shop, id, "deliver", Map.of("pin", pin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.customer").doesNotExist());
        merchantAction(shop, id, "deliver", Map.of("pin", pin)).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.deliveryPin").doesNotExist())
                .andExpect(jsonPath("$.history.length()").value(5));
        assertThat(stockOf(product)).isEqualTo(8);
    }

    @Test
    void pickupIsConfirmedWithPinOnceReady() throws Exception {
        Shop shop = openShop("Panaderia Central");
        UUID product = addProduct(shop, "Pan", 500, null);
        JsonNode order = placed(register(Role.CUSTOMER), orderData(shop, product, 4, "PICKUP", 2000));
        String id = order.get("id").asText();
        assertThat(order.get("deliveryAddress").isNull()).isTrue();

        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5)).andExpect(status().isOk());
        merchantAction(shop, id, "ready", Map.of()).andExpect(status().isOk());
        merchantAction(shop, id, "dispatch", Map.of()).andExpect(status().isConflict());
        merchantAction(shop, id, "deliver", Map.of("pin", order.get("deliveryPin").asText()))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void pinLocksAfterFiveFailuresUntilTheCustomerRenewsIt() throws Exception {
        Shop shop = openShop("El Fogon");
        UUID product = addProduct(shop, "Almuerzo", 12000, null);
        Account customer = register(Role.CUSTOMER);
        JsonNode order = placed(customer, orderData(shop, product, 1, "PICKUP", 12000));
        String id = order.get("id").asText();
        String pin = order.get("deliveryPin").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5));
        merchantAction(shop, id, "ready", Map.of());

        for (int i = 0; i < 5; i++) {
            merchantAction(shop, id, "deliver", Map.of("pin", otherThan(pin))).andExpect(status().isBadRequest());
        }
        merchantAction(shop, id, "deliver", Map.of("pin", pin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PIN_LOCKED"));

        String renewed = body(mvc.perform(post("/api/v1/orders/" + id + "/new-pin")
                .header("Authorization", customer.bearer())).andReturn()).get("deliveryPin").asText();
        merchantAction(shop, id, "deliver", Map.of("pin", renewed))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void theCustomerOnlyPaysTheTotalTheySaw() throws Exception {
        Shop shop = openShop("Cafe Real");
        UUID product = addProduct(shop, "Cafe", 3000, 5);
        Account customer = register(Role.CUSTOMER);

        place(customer, orderData(shop, product, 1, "DELIVERY", 3000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRICE_CHANGED"));

        // El rechazo no deja existencias reservadas ni pedidos a medias
        assertThat(stockOf(product)).isEqualTo(5);
        mvc.perform(get("/api/v1/orders").header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void repeatingAConfirmationDoesNotCreateASecondOrder() throws Exception {
        Shop shop = openShop("Dulces Ana");
        UUID product = addProduct(shop, "Bocadillo", 2000, 5);
        Account customer = register(Role.CUSTOMER);
        var order = orderData(shop, product, 1, "PICKUP", 2000);
        String key = UUID.randomUUID().toString();

        String first = body(place(customer, order, key).andReturn()).get("id").asText();
        String second = body(place(customer, order, key).andReturn()).get("id").asText();

        assertThat(second).isEqualTo(first);
        assertThat(stockOf(product)).isEqualTo(4);
        mvc.perform(postJson("/api/v1/orders", order).header("Authorization", customer.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void theLastUnitIsSoldOnlyOnce() throws Exception {
        Shop shop = openShop("Ultima Unidad");
        UUID product = addProduct(shop, "Torta", 30000, 1);
        List<Account> customers = List.of(register(Role.CUSTOMER), register(Role.CUSTOMER),
                register(Role.CUSTOMER), register(Role.CUSTOMER));
        CountDownLatch start = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(customers.size())) {
            List<Future<Integer>> results = IntStream.range(0, customers.size())
                    .mapToObj(i -> pool.submit(() -> {
                        start.await();
                        return place(customers.get(i), orderData(shop, product, 1, "PICKUP", 30000))
                                .andReturn().getResponse().getStatus();
                    }))
                    .toList();
            start.countDown();
            List<Integer> statuses = new java.util.ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409, 409, 409);
        }
        assertThat(stockOf(product)).isZero();
    }

    @Test
    void rejectionAndCancellationReturnTheStock() throws Exception {
        Shop shop = openShop("Surtidora");
        UUID product = addProduct(shop, "Arroz", 4000, 10);
        Account customer = register(Role.CUSTOMER);
        String rejected = placed(customer, orderData(shop, product, 3, "PICKUP", 12000)).get("id").asText();
        String cancelled = placed(customer, orderData(shop, product, 2, "PICKUP", 8000)).get("id").asText();
        String late = placed(customer, orderData(shop, product, 1, "PICKUP", 4000)).get("id").asText();
        assertThat(stockOf(product)).isEqualTo(4);

        merchantAction(shop, rejected, "reject", Map.of("reason", "no")).andExpect(status().isBadRequest());
        merchantAction(shop, rejected, "reject", Map.of("reason", "Sin domicilios hoy"))
                .andExpect(jsonPath("$.statusReason").value("Sin domicilios hoy"));
        mvc.perform(post("/api/v1/orders/" + cancelled + "/cancel").header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stockOf(product)).isEqualTo(9);

        // En preparacion ya no lo cancela el cliente; el comercio si, con motivo
        merchantAction(shop, late, "accept", Map.of("prepMinutes", 10));
        mvc.perform(post("/api/v1/orders/" + late + "/cancel").header("Authorization", customer.bearer()))
                .andExpect(status().isConflict());
        merchantAction(shop, late, "cancel", Map.of("reason", "Se agoto el gas"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stockOf(product)).isEqualTo(10);
    }

    @Test
    void nobodySeesOrActsOnOtherPeoplesOrders() throws Exception {
        Shop shop = openShop("Propio");
        Shop rival = openShop("Rival");
        UUID product = addProduct(shop, "Jugo", 5000, null);
        Account customer = register(Role.CUSTOMER);
        Account stranger = register(Role.CUSTOMER);
        String id = placed(customer, orderData(shop, product, 1, "PICKUP", 5000)).get("id").asText();

        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", stranger.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/orders/" + id + "/cancel").header("Authorization", stranger.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/orders/" + id + "/new-pin").header("Authorization", stranger.bearer()))
                .andExpect(status().isNotFound());
        merchantAction(rival, id, "accept", Map.of("prepMinutes", 5)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/merchant/orders").header("Authorization", rival.owner().bearer()))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/merchant/orders").header("Authorization", customer.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", shop.owner().bearer()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "CUSTOMER", mode = EnumSource.Mode.EXCLUDE)
    void onlyCustomersOrder(Role role) throws Exception {
        mvc.perform(get("/api/v1/orders").header("Authorization", accountWithRole(role).bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void productsFromAnotherShopCannotBeMixed() throws Exception {
        Shop shop = openShop("Uno");
        Shop other = openShop("Dos");
        UUID foreign = addProduct(other, "Ajeno", 1000, 5);

        place(register(Role.CUSTOMER), orderData(shop, foreign, 1, "PICKUP", 1000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_UNAVAILABLE"));
        assertThat(stockOf(foreign)).isEqualTo(5);
    }

    @Test
    void closedShopsAndInvalidQuantitiesAreRefused() throws Exception {
        Shop shop = openShop("Cerrado");
        UUID product = addProduct(shop, "Algo", 1000, 3);
        Account customer = register(Role.CUSTOMER);

        place(customer, orderData(shop, product, 0, "PICKUP", 0)).andExpect(status().isBadRequest());
        place(customer, orderData(shop, product, -1, "PICKUP", -1000)).andExpect(status().isBadRequest());
        place(customer, orderData(shop, product, 4, "PICKUP", 4000))
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
        setAccepting(shop.owner(), false);
        place(customer, orderData(shop, product, 1, "PICKUP", 1000))
                .andExpect(jsonPath("$.code").value("BUSINESS_CLOSED"));
    }

    @Test
    void orderHistoryCannotBeRewritten() throws Exception {
        Shop shop = openShop("Historial");
        placed(register(Role.CUSTOMER), orderData(shop, addProduct(shop, "Producto", 1000, null), 1, "PICKUP", 1000));

        for (String statement : List.of("delete from order_events", "update order_events set note = 'x'")) {
            assertThatThrownBy(() -> jdbc.execute(statement))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("solo insercion");
        }
    }
}
