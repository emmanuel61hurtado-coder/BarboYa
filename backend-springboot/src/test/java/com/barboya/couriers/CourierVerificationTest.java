package com.barboya.couriers;

import com.barboya.support.IntegrationTest;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourierVerificationTest extends IntegrationTest {

    private static final AtomicInteger DOCUMENT_NUMBERS = new AtomicInteger(1_000_000);

    @Autowired private JdbcTemplate jdbc;

    private static byte[] image() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private Map<String, Object> profile(String vehicle, String number) {
        Map<String, Object> data = new HashMap<>();
        data.put("documentNumber", number);
        data.put("vehicleType", vehicle);
        data.put("plate", vehicle.equals("BICYCLE") ? "" : "ABC12D");
        data.put("acceptsDataProcessing", true);
        return data;
    }

    private ResultActions saveProfile(Account courier, Map<String, Object> data) throws Exception {
        return mvc.perform(put("/api/v1/courier/profile").header("Authorization", courier.bearer())
                .contentType("application/json").content(json.writeValueAsString(data)));
    }

    private Account courierWithProfile(String vehicle) throws Exception {
        Account courier = register(Role.COURIER);
        saveProfile(courier, profile(vehicle, "" + DOCUMENT_NUMBERS.incrementAndGet())).andExpect(status().isOk());
        return courier;
    }

    private ResultActions upload(Account courier, String type, byte[] bytes, LocalDate expiresOn) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/courier/documents/" + type)
                .file(new MockMultipartFile("file", "archivo.png", "image/png", bytes));
        if (expiresOn != null) {
            request.param("expiresOn", expiresOn.toString());
        }
        return mvc.perform(request.header("Authorization", courier.bearer()));
    }

    private LocalDate nextYear() {
        return LocalDate.now().plusYears(1);
    }

    private String documentId(JsonNode view, String type) {
        for (JsonNode document : view.get("documents")) {
            if (document.get("type").asText().equals(type)) {
                return document.get("id").asText();
            }
        }
        throw new AssertionError("no esta " + type);
    }

    private JsonNode uploadBicycleDocuments(Account courier) throws Exception {
        upload(courier, "IDENTITY", image(), null).andExpect(status().isOk());
        return body(upload(courier, "FACE_PHOTO", image(), null).andExpect(status().isOk()).andReturn());
    }

    private ResultActions review(Account admin, String document, String decision, String note) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("decision", decision);
        data.put("note", note);
        return mvc.perform(postJson("/api/v1/admin/documents/" + document + "/review", data)
                .header("Authorization", admin.bearer()));
    }

    private ResultActions activate(Account admin, Account courier) throws Exception {
        return mvc.perform(patch("/api/v1/admin/users/" + courier.id() + "/status")
                .header("Authorization", admin.bearer()).contentType("application/json")
                .content("{\"status\":\"ACTIVE\",\"reason\":\"Documentos revisados\"}"));
    }

    private Account approvedCyclist(Account admin) throws Exception {
        Account courier = courierWithProfile("BICYCLE");
        JsonNode view = uploadBicycleDocuments(courier);
        review(admin, documentId(view, "IDENTITY"), "APPROVED", null);
        review(admin, documentId(view, "FACE_PHOTO"), "APPROVED", null);
        activate(admin, courier).andExpect(status().isOk());
        return courier;
    }

    private String accountStatus(Account account) throws Exception {
        return body(mvc.perform(get("/api/v1/users/me").header("Authorization", account.bearer())).andReturn())
                .get("status").asText();
    }

    @Test
    void aCourierIsActivatedOnlyWhenEveryDocumentIsApproved() throws Exception {
        Account admin = accountWithRole(Role.ADMIN);
        Account courier = register(Role.COURIER);
        activate(admin, courier).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENTS_INCOMPLETE"));

        saveProfile(courier, profile("MOTORCYCLE", "52000111")).andExpect(jsonPath("$.pending.length()").value(4));
        upload(courier, "IDENTITY", image(), null);
        upload(courier, "FACE_PHOTO", image(), null);
        upload(courier, "DRIVER_LICENSE", image(), nextYear());
        JsonNode view = body(upload(courier, "SOAT", image(), nextYear()).andReturn());
        activate(admin, courier).andExpect(status().isConflict());

        for (String type : new String[]{"IDENTITY", "FACE_PHOTO", "DRIVER_LICENSE"}) {
            review(admin, documentId(view, type), "APPROVED", null).andExpect(status().isOk());
        }
        review(admin, documentId(view, "SOAT"), "REJECTED", "no").andExpect(status().isBadRequest());
        review(admin, documentId(view, "SOAT"), "REJECTED", "La foto esta borrosa")
                .andExpect(jsonPath("$.pending[0]").value("SOAT (rechazado)"));
        activate(admin, courier).andExpect(status().isConflict());

        JsonNode renewed = body(upload(courier, "SOAT", image(), nextYear()).andReturn());
        // La revision anterior no se traslada al archivo nuevo
        review(admin, documentId(view, "SOAT"), "APPROVED", null).andExpect(status().isNotFound());
        review(admin, documentId(renewed, "SOAT"), "APPROVED", null)
                .andExpect(jsonPath("$.pending").isEmpty());
        activate(admin, courier).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anExpiredDocumentBlocksActivation() throws Exception {
        Account admin = accountWithRole(Role.ADMIN);
        Account courier = courierWithProfile("CAR");
        upload(courier, "IDENTITY", image(), null);
        upload(courier, "FACE_PHOTO", image(), null);
        upload(courier, "DRIVER_LICENSE", image(), nextYear());
        JsonNode view = body(upload(courier, "SOAT", image(), LocalDate.now().plusDays(2)).andReturn());
        for (JsonNode document : view.get("documents")) {
            review(admin, document.get("id").asText(), "APPROVED", null);
        }

        clock.advance(Duration.ofDays(4));
        Account laterAdmin = accountWithRole(Role.ADMIN);

        activate(laterAdmin, courier).andExpect(status().isConflict());
        review(laterAdmin, documentId(view, "SOAT"), "APPROVED", null)
                .andExpect(jsonPath("$.code").value("DOCUMENT_EXPIRED"));
    }

    @Test
    void changingWhatWasApprovedSendsTheAccountBackToReview() throws Exception {
        Account admin = accountWithRole(Role.ADMIN);
        Account courier = approvedCyclist(admin);
        assertThat(accountStatus(courier)).isEqualTo("ACTIVE");

        upload(courier, "FACE_PHOTO", image(), null).andExpect(status().isOk());
        assertThat(accountStatus(courier)).isEqualTo("PENDING_VERIFICATION");

        Account other = approvedCyclist(admin);
        saveProfile(other, profile("CAR", "" + DOCUMENT_NUMBERS.incrementAndGet()));
        assertThat(accountStatus(other)).isEqualTo("PENDING_VERIFICATION");
    }

    @ParameterizedTest
    @EnumSource(value = DocumentType.class, names = {"DRIVER_LICENSE", "SOAT", "TECHNICAL_REVIEW"})
    void vehicleDocumentsNeedAValidExpiry(DocumentType type) throws Exception {
        Account courier = courierWithProfile("MOTORCYCLE");

        upload(courier, type.name(), image(), null).andExpect(jsonPath("$.code").value("INVALID_EXPIRY"));
        upload(courier, type.name(), image(), LocalDate.now().minusDays(2))
                .andExpect(jsonPath("$.code").value("INVALID_EXPIRY"));
    }

    @Test
    void filesAreJudgedByTheirContent() throws Exception {
        Account courier = courierWithProfile("BICYCLE");
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        byte[] fakeImage = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        byte[] pdf = "%PDF-1.7 contenido".getBytes(StandardCharsets.UTF_8);
        byte[] huge = new byte[5 * 1024 * 1024 + 1];

        for (byte[] bad : new byte[][]{html, fakeImage, huge, new byte[0]}) {
            upload(courier, "IDENTITY", bad, null).andExpect(jsonPath("$.code").value("INVALID_FILE"));
        }
        upload(courier, "FACE_PHOTO", pdf, null).andExpect(jsonPath("$.code").value("INVALID_FILE"));
        upload(courier, "IDENTITY", pdf, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].contentType").value("application/pdf"));
        upload(courier, "SOAT", image(), nextYear()).andExpect(jsonPath("$.code").value("DOCUMENT_NOT_APPLICABLE"));
    }

    @Test
    void consentAndAUniqueIdentityAreRequired() throws Exception {
        Account first = register(Role.COURIER);
        Account second = register(Role.COURIER);
        upload(first, "IDENTITY", image(), null).andExpect(jsonPath("$.code").value("PROFILE_REQUIRED"));
        var withoutConsent = profile("BICYCLE", "80123456");
        withoutConsent.put("acceptsDataProcessing", false);
        saveProfile(first, withoutConsent).andExpect(status().isBadRequest());
        var withoutPlate = profile("MOTORCYCLE", "80123456");
        withoutPlate.put("plate", "");
        saveProfile(first, withoutPlate).andExpect(jsonPath("$.code").value("PLATE_REQUIRED"));

        saveProfile(first, profile("BICYCLE", "80123456")).andExpect(status().isOk());
        saveProfile(second, profile("BICYCLE", "80123456"))
                .andExpect(jsonPath("$.code").value("DOCUMENT_NUMBER_TAKEN"));
    }

    @Test
    void documentsAreEncryptedPrivateAndTheirViewsAudited() throws Exception {
        Account courier = courierWithProfile("BICYCLE");
        byte[] original = image();
        String id = documentId(body(upload(courier, "IDENTITY", original, null).andReturn()), "IDENTITY");
        String key = jdbc.queryForObject("select storage_key from courier_documents where id = ?",
                String.class, UUID.fromString(id));
        Path file = Path.of(System.getProperty("java.io.tmpdir"), "barboya-test-files", key + ".bin");
        assertThat(Files.readAllBytes(file)).hasSizeGreaterThan(original.length);
        assertThat(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1)).doesNotContain("PNG", "IHDR");

        mvc.perform(get("/api/v1/courier/documents/" + id + "/file").header("Authorization", courier.bearer()))
                .andExpect(content().bytes(original))
                .andExpect(header().string("Cache-Control", "no-store"));

        Account stranger = courierWithProfile("BICYCLE");
        mvc.perform(get("/api/v1/courier/documents/" + id + "/file").header("Authorization", stranger.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/courier/documents/" + id).header("Authorization", stranger.bearer()))
                .andExpect(status().isNotFound());
        for (Role role : new Role[]{Role.CUSTOMER, Role.RESTAURANT}) {
            Account actor = register(role);
            mvc.perform(get("/api/v1/admin/documents/" + id + "/file").header("Authorization", actor.bearer()))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/courier/profile").header("Authorization", actor.bearer()))
                    .andExpect(status().isForbidden());
        }

        Account admin = accountWithRole(Role.ADMIN);
        mvc.perform(get("/api/v1/admin/documents/" + id + "/file").header("Authorization", admin.bearer()))
                .andExpect(content().bytes(original));
        assertThat(jdbc.queryForObject(
                "select count(*) from audit_events where action = 'DOCUMENT_VIEWED' and actor_id = ?",
                Integer.class, admin.id())).isEqualTo(1);

        mvc.perform(delete("/api/v1/courier/documents/" + id).header("Authorization", courier.bearer()))
                .andExpect(jsonPath("$.documents").isEmpty());
        assertThat(file).doesNotExist();
    }
}
