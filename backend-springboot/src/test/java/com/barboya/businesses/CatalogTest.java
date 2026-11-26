package com.barboya.businesses;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogTest extends CommerceFixtures {

    private static String token() {
        return "Zq" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void pendingAccountsPrepareTheirShopButStayHidden() throws Exception {
        Account owner = register(Role.STORE);
        Account customer = register(Role.CUSTOMER);
        String name = token();

        mvc.perform(get("/api/v1/merchant/business").header("Authorization", owner.bearer()))
                .andExpect(status().isNotFound());
        String id = body(saveBusiness(owner, businessData(name)).andExpect(status().isOk()).andReturn())
                .get("id").asText();
        setAccepting(owner, true)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_APPROVED"));

        mvc.perform(get("/api/v1/catalog/businesses?q=" + name).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/catalog/businesses/" + id).header("Authorization", customer.bearer()))
                .andExpect(status().isNotFound());

        // El equipo revisa los datos y aprueba
        mvc.perform(get("/api/v1/admin/users/" + owner.id() + "/business")
                        .header("Authorization", accountWithRole(Role.ADMIN).bearer()))
                .andExpect(jsonPath("$.name").value(name));
        approve(owner);
        mvc.perform(get("/api/v1/catalog/businesses?q=" + name).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].open").value(false))
                .andExpect(jsonPath("$.items[0].nit").doesNotExist());
    }

    @Test
    void searchMatchesShopAndProductNames() throws Exception {
        Shop shop = openShop("Tienda " + token());
        String product = token();
        addProduct(shop, "Empanada " + product, 2000, 0);
        Account customer = register(Role.CUSTOMER);

        mvc.perform(get("/api/v1/catalog/businesses?q=" + product.toUpperCase())
                        .header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(shop.businessId().toString()));
        mvc.perform(get("/api/v1/catalog/businesses?q=" + product + "&category=PHARMACY")
                        .header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.total").value(0));
        // Un comodin escrito por el usuario no lista todo
        mvc.perform(get("/api/v1/catalog/businesses?q=%25%25%25").header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/catalog/businesses/" + shop.businessId())
                        .header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.products[0].soldOut").value(true))
                .andExpect(jsonPath("$.products[0].stock").doesNotExist());
    }

    @Test
    void onlyTheOwnerManagesAProduct() throws Exception {
        Shop shop = openShop("Duenos " + token());
        Shop rival = openShop("Otros " + token());
        UUID product = addProduct(shop, "Original", 5000, 2);
        String change = json.writeValueAsString(
                Map.of("name", "Alterado", "category", "General", "priceCop", 100, "available", true));

        mvc.perform(put("/api/v1/merchant/products/" + product).header("Authorization", rival.owner().bearer())
                .contentType("application/json").content(change)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/merchant/products/" + product).header("Authorization", rival.owner().bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/merchant/products/" + product).header("Authorization", shop.owner().bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/merchant/products").header("Authorization", shop.owner().bearer()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidShopDataIsRejected() throws Exception {
        Account owner = register(Role.RESTAURANT);
        var noFulfillment = businessData("Sin entrega");
        noFulfillment.put("pickupEnabled", false);
        noFulfillment.put("deliveryEnabled", false);
        saveBusiness(owner, noFulfillment).andExpect(status().isBadRequest());
        var negativeFee = businessData("Tarifa negativa");
        negativeFee.put("deliveryFeeCop", -1);
        saveBusiness(owner, negativeFee).andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/merchant/products").header("Authorization", register(Role.CUSTOMER).bearer()))
                .andExpect(status().isForbidden());
    }
}
