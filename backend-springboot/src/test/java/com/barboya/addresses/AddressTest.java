package com.barboya.addresses;

import com.barboya.support.IntegrationTest;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressTest extends IntegrationTest {

    private static final String BASE = "/api/v1/addresses";

    private static Map<String, Object> data(String label) {
        Map<String, Object> data = new HashMap<>();
        data.put("label", label);
        data.put("address", "Carrera 7 # 10-15");
        data.put("notes", " ");
        data.put("latitude", 5.93);
        data.put("longitude", -73.61);
        return data;
    }

    private ResultActions add(Account who, Map<String, Object> data) throws Exception {
        return mvc.perform(postJson(BASE, data).header("Authorization", who.bearer()));
    }

    private String added(Account who, String label) throws Exception {
        return body(add(who, data(label)).andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    private ResultActions update(Account who, String id, Map<String, Object> data) throws Exception {
        return mvc.perform(put(BASE + "/" + id).header("Authorization", who.bearer())
                .contentType("application/json").content(json.writeValueAsString(data)));
    }

    private ResultActions list(Account who) throws Exception {
        return mvc.perform(get(BASE).header("Authorization", who.bearer()));
    }

    @Test
    void aCustomerKeepsEditsAndDeletesTheirAddresses() throws Exception {
        Account customer = register(Role.CUSTOMER);
        String home = added(customer, "  Casa ");
        added(customer, "Trabajo");

        list(customer).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("Casa"))
                .andExpect(jsonPath("$[0].notes").doesNotExist())
                .andExpect(jsonPath("$[0].latitude").value(5.93));

        add(customer, data("casa")).andExpect(jsonPath("$.code").value("ADDRESS_LABEL_TAKEN"));
        update(customer, home, data("TRABAJO")).andExpect(jsonPath("$.code").value("ADDRESS_LABEL_TAKEN"));
        Map<String, Object> moved = data("Casa");
        moved.put("address", "Calle 9 # 8-20");
        moved.put("latitude", null);
        moved.put("longitude", null);
        update(customer, home, moved).andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Calle 9 # 8-20"))
                .andExpect(jsonPath("$.latitude").doesNotExist());

        mvc.perform(delete(BASE + "/" + home).header("Authorization", customer.bearer()))
                .andExpect(status().isNoContent());
        list(customer).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void nobodyElseReachesAnAddress() throws Exception {
        Account owner = register(Role.CUSTOMER);
        Account other = register(Role.CUSTOMER);
        String id = added(owner, "Casa");

        list(other).andExpect(jsonPath("$.length()").value(0));
        update(other, id, data("Mia")).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/" + id).header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());
        list(register(Role.RESTAURANT)).andExpect(status().isForbidden());
        list(accountWithRole(Role.ADMIN)).andExpect(status().isForbidden());
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        list(owner).andExpect(jsonPath("$[0].label").value("Casa"));
    }

    @Test
    void theDataIsValidatedAndThereIsALimit() throws Exception {
        Account customer = register(Role.CUSTOMER);
        Map<String, Object> half = data("Casa");
        half.put("longitude", null);
        add(customer, half).andExpect(jsonPath("$.code").value("INVALID_LOCATION"));
        Map<String, Object> shortAddress = data("Casa");
        shortAddress.put("address", "x");
        add(customer, shortAddress).andExpect(status().isBadRequest());

        for (int i = 0; i < 10; i++) {
            added(customer, "Lugar " + i);
        }
        add(customer, data("Una mas")).andExpect(jsonPath("$.code").value("TOO_MANY_ADDRESSES"));
    }
}
