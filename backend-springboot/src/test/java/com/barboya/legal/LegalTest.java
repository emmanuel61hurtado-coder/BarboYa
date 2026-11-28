package com.barboya.legal;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autorizaciones, documentos publicos, identificacion de comercios y acuse de recibo. */
class LegalTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private Map<String, Object> registration() {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", "Persona de Prueba");
        data.put("email", uniqueEmail());
        data.put("phone", uniquePhone());
        data.put("password", PASSWORD);
        data.put("role", "CUSTOMER");
        data.put("adult", true);
        data.put("termsVersion", LegalDocument.TERMS.version());
        data.put("privacyVersion", LegalDocument.PRIVACY.version());
        return data;
    }

    private void rejected(Map<String, Object> data, String code) throws Exception {
        mvc.perform(postJson("/api/v1/auth/register", data))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code));
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class,
                data.get("email"))).isZero();
    }

    @Test
    void nobodyRegistersWithoutAuthorizingAndDeclaringTheirAge() throws Exception {
        for (Object adult : new Object[]{null, false}) {
            Map<String, Object> data = registration();
            data.put("adult", adult);
            rejected(data, "ADULT_REQUIRED");
        }
        for (String field : new String[]{"termsVersion", "privacyVersion"}) {
            Map<String, Object> missing = registration();
            missing.remove(field);
            rejected(missing, "CONSENT_REQUIRED");
            Map<String, Object> old = registration();
            old.put(field, "version-anterior");
            rejected(old, "CONSENT_REQUIRED");
        }
    }

    @Test
    void theAuthorizationIsKeptAsProofThatCannotBeRewritten() throws Exception {
        Account customer = register(Role.CUSTOMER);

        assertThat(jdbc.queryForList(
                "select document from legal_acceptances where user_id = ? order by document", String.class,
                customer.id())).containsExactly("ADULT", "PRIVACY", "TERMS");
        assertThat(jdbc.queryForObject("select version from legal_acceptances where user_id = ? "
                + "and document = 'PRIVACY'", String.class, customer.id()))
                .isEqualTo(LegalDocument.PRIVACY.version());
        for (String statement : new String[]{
                "update legal_acceptances set version = 'x'", "delete from legal_acceptances",
                "truncate legal_acceptances"}) {
            assertThatThrownBy(() -> jdbc.execute(statement)).isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    void whoeverAcceptedAnOlderVersionIsAskedAgain() throws Exception {
        // Una cuenta creada sin pasar por el registro no ha aceptado nada
        Account staff = accountWithRole(Role.ADMIN);
        String url = "/api/v1/legal/pending";
        mvc.perform(get(url).header("Authorization", staff.bearer()))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(postJson("/api/v1/legal/acceptances",
                        Map.of("termsVersion", "x", "privacyVersion", "y")).header("Authorization", staff.bearer()))
                .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
        Map<String, String> current = Map.of("termsVersion", LegalDocument.TERMS.version(),
                "privacyVersion", LegalDocument.PRIVACY.version());
        for (int i = 0; i < 2; i++) {
            mvc.perform(postJson("/api/v1/legal/acceptances", current).header("Authorization", staff.bearer()))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get(url).header("Authorization", staff.bearer())).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void theDocumentsArePublicAndShowWhatIsStillMissing() throws Exception {
        mvc.perform(get("/api/v1/legal"))
                .andExpect(jsonPath("$.reviewed").value(false))
                .andExpect(jsonPath("$.termsVersion").value(LegalDocument.TERMS.version()));
        for (String slug : new String[]{"terminos", "privacidad"}) {
            String body = body(mvc.perform(get("/api/v1/legal/documents/" + slug))
                    .andExpect(status().isOk()).andReturn()).get("body").asText();
            assertThat(body).contains("[PENDIENTE: nombre del responsable]").doesNotContain("{{");
        }
        mvc.perform(get("/api/v1/legal/documents/otro")).andExpect(status().isNotFound());
    }

    @Test
    void aShopMustIdentifyItselfAndTheBuyerSeesWhoSells() throws Exception {
        Account owner = register(Role.RESTAURANT);
        approve(owner);
        for (String field : new String[]{"legalName", "nit", "notificationAddress", "contactEmail"}) {
            Map<String, Object> data = businessData("Sin " + field);
            data.remove(field);
            saveBusiness(owner, data).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors." + field).exists());
        }

        Shop shop = openShop("Identificado " + UUID.randomUUID());
        Account customer = register(Role.CUSTOMER);
        mvc.perform(get("/api/v1/catalog/businesses/" + shop.businessId())
                        .header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.seller.legalName").value("Responsable de Prueba"))
                .andExpect(jsonPath("$.seller.identification").value("900123456-7"))
                .andExpect(jsonPath("$.seller.notificationAddress").value("Carrera 1 # 2-3, Barbosa"));

        // Un comercio anterior a esta exigencia no puede recibir pedidos hasta completarla
        jdbc.update("update businesses set contact_email = null, accepting_orders = false where id = ?",
                shop.businessId());
        setAccepting(shop.owner(), true).andExpect(jsonPath("$.code").value("IDENTIFICATION_REQUIRED"));
    }

    @Test
    void theCustomerGetsAReceiptByMail() throws Exception {
        Shop shop = openShop("Recibo " + UUID.randomUUID());
        UUID product = addProduct(shop, "Tamal", 7000, null);
        Account customer = register(Role.CUSTOMER);
        long number = placed(customer, orderData(shop, product, 2, "PICKUP", 14000)).get("number").asLong();

        assertThat(mailsTo(customer.email()))
                .extracting(SimpleMailMessage::getSubject).contains("Confirmacion de tu pedido " + number);
        assertThat(mailsTo(customer.email()).getLast().getText())
                .contains("2 x Tamal: $14000", "900123456-7", "impuestos incluidos", "Retracto", "sic.gov.co")
                .doesNotContain("PIN");
    }
}
