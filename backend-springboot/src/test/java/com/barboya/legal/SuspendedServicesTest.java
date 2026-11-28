package com.barboya.legal;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Configuracion de fabrica: sin farmacias ni registro de transporte de pasajeros. */
@TestPropertySource(properties = {"barboya.features.pharmacy=false", "barboya.features.driver-registration=false"})
class SuspendedServicesTest extends CommerceFixtures {

    @Test
    void passengerDriversCannotRegister() throws Exception {
        for (Role role : new Role[]{Role.MOTO_DRIVER, Role.CAR_DRIVER}) {
            mvc.perform(postJson("/api/v1/auth/register", Map.of(
                            "fullName", "Persona de Prueba", "email", uniqueEmail(), "phone", uniquePhone(),
                            "password", PASSWORD, "role", role.name(), "adult", true,
                            "termsVersion", LegalDocument.TERMS.version(),
                            "privacyVersion", LegalDocument.PRIVACY.version())))
                    .andExpect(jsonPath("$.code").value("ROLE_UNAVAILABLE"));
        }
        register(Role.COURIER);
        mvc.perform(get("/api/v1/legal"))
                .andExpect(jsonPath("$.driverRegistration").value(false))
                .andExpect(jsonPath("$.pharmacy").value(false));
    }

    @Test
    void noPharmacyCanBeRegistered() throws Exception {
        Account owner = register(Role.STORE);
        Map<String, Object> data = businessData("Drogueria de prueba");
        data.put("category", "PHARMACY");

        saveBusiness(owner, data).andExpect(jsonPath("$.code").value("CATEGORY_UNAVAILABLE"));
    }
}
