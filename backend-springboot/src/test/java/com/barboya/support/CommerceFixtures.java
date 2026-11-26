package com.barboya.support;

import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comercio, productos y pedidos creados siempre a traves de la API. */
public abstract class CommerceFixtures extends IntegrationTest {

    public record Shop(Account owner, UUID businessId) {
    }

    protected void approve(Account account) {
        User user = users.findById(account.id()).orElseThrow();
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);
    }

    protected Map<String, Object> businessData(String name) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("category", "RESTAURANT");
        data.put("address", "Calle 9 # 8-20, Barbosa");
        data.put("phone", "6077480000");
        data.put("pickupEnabled", true);
        data.put("deliveryEnabled", true);
        data.put("deliveryFeeCop", 3000);
        return data;
    }

    protected ResultActions saveBusiness(Account owner, Map<String, Object> data) throws Exception {
        return mvc.perform(put("/api/v1/merchant/business")
                .header("Authorization", owner.bearer())
                .contentType("application/json")
                .content(json.writeValueAsString(data)));
    }

    protected ResultActions setAccepting(Account owner, boolean accepting) throws Exception {
        return mvc.perform(patch("/api/v1/merchant/business/accepting")
                .header("Authorization", owner.bearer())
                .contentType("application/json")
                .content("{\"accepting\":" + accepting + "}"));
    }

    /** Comercio aprobado y recibiendo pedidos. */
    protected Shop openShop(String name) throws Exception {
        Account owner = register(Role.RESTAURANT);
        approve(owner);
        JsonNode saved = body(saveBusiness(owner, businessData(name)).andExpect(status().isOk()).andReturn());
        setAccepting(owner, true).andExpect(status().isOk());
        return new Shop(owner, UUID.fromString(saved.get("id").asText()));
    }

    protected UUID addProduct(Shop shop, String name, int price, Integer stock) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("category", "General");
        data.put("priceCop", price);
        data.put("stock", stock);
        data.put("available", true);
        JsonNode saved = body(mvc.perform(postJson("/api/v1/merchant/products", data)
                        .header("Authorization", shop.owner().bearer()))
                .andExpect(status().isCreated()).andReturn());
        return UUID.fromString(saved.get("id").asText());
    }

    protected Map<String, Object> orderData(Shop shop, UUID product, int quantity, String fulfillment,
                                            int expectedTotal) {
        Map<String, Object> data = new HashMap<>();
        data.put("businessId", shop.businessId());
        data.put("items", List.of(Map.of("productId", product, "quantity", quantity)));
        data.put("fulfillment", fulfillment);
        data.put("deliveryAddress", "Carrera 7 # 10-15");
        data.put("paymentMethod", "CASH");
        data.put("expectedTotalCop", expectedTotal);
        return data;
    }

    protected ResultActions place(Account customer, Map<String, Object> order, String key) throws Exception {
        return mvc.perform(postJson("/api/v1/orders", order)
                .header("Authorization", customer.bearer())
                .header("Idempotency-Key", key));
    }

    protected ResultActions place(Account customer, Map<String, Object> order) throws Exception {
        return place(customer, order, UUID.randomUUID().toString());
    }

    protected JsonNode placed(Account customer, Map<String, Object> order) throws Exception {
        return body(place(customer, order).andExpect(status().isCreated()).andReturn());
    }

    protected ResultActions merchantAction(Shop shop, String orderId, String action, Map<String, ?> data)
            throws Exception {
        return mvc.perform(postJson("/api/v1/merchant/orders/" + orderId + "/" + action, data)
                .header("Authorization", shop.owner().bearer()));
    }
}
