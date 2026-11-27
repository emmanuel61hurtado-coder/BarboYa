package com.barboya.businesses;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogPhotoAndHoursTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private static byte[] image() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private ResultActions upload(Account owner, UUID product, byte[] file) throws Exception {
        return mvc.perform(multipart("/api/v1/merchant/products/" + product + "/photo")
                .file(new MockMultipartFile("file", "foto.png", "image/png", file))
                .header("Authorization", owner.bearer()));
    }

    private String uploadedUrl(Account owner, UUID product) throws Exception {
        return body(upload(owner, product, image()).andExpect(status().isOk()).andReturn())
                .get("photoUrl").asText();
    }

    private ResultActions hours(Account owner, List<Map<String, Object>> week) throws Exception {
        return mvc.perform(put("/api/v1/merchant/business/hours").header("Authorization", owner.bearer())
                .contentType("application/json").content(json.writeValueAsString(Map.of("hours", week))));
    }

    /** Turno de una hora que empieza dentro de {@code fromNow} horas, hora de Colombia. */
    private Map<String, Object> shift(int fromNow, int length) {
        LocalDateTime start = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("America/Bogota"))
                .plusHours(fromNow);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
        return Map.of("day", start.getDayOfWeek().getValue(), "opens", start.format(format),
                "closes", start.plusHours(length).format(format));
    }

    private ResultActions store(Account customer, Shop shop) throws Exception {
        return mvc.perform(get("/api/v1/catalog/businesses/" + shop.businessId())
                .header("Authorization", customer.bearer()));
    }

    @Test
    void aProductPhotoIsPublicAndTheOldOneDisappears() throws Exception {
        Shop shop = openShop("Con fotos " + UUID.randomUUID());
        UUID product = addProduct(shop, "Torta", 5000, null);
        Account customer = register(Role.CUSTOMER);

        String first = uploadedUrl(shop.owner(), product);
        store(customer, shop).andExpect(jsonPath("$.products[0].photoUrl").value(first));
        mvc.perform(get(first))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        String second = uploadedUrl(shop.owner(), product);
        assertThat(second).isNotEqualTo(first);
        mvc.perform(get(first)).andExpect(status().isNotFound());
        mvc.perform(get(second)).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/merchant/products/" + product + "/photo")
                        .header("Authorization", shop.owner().bearer()))
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
        mvc.perform(get(second)).andExpect(status().isNotFound());
    }

    @Test
    void archivingAProductRemovesItsPhoto() throws Exception {
        Shop shop = openShop("Archiva " + UUID.randomUUID());
        UUID product = addProduct(shop, "Pastel", 5000, null);
        String url = uploadedUrl(shop.owner(), product);

        mvc.perform(delete("/api/v1/merchant/products/" + product)
                .header("Authorization", shop.owner().bearer())).andExpect(status().isNoContent());

        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void onlyTheOwnerUploadsImagesAndPrivateFilesNeverComeOutAsPhotos() throws Exception {
        Shop shop = openShop("Propio " + UUID.randomUUID());
        Shop other = openShop("Ajeno " + UUID.randomUUID());
        UUID product = addProduct(shop, "Jugo", 4000, null);

        upload(other.owner(), product, image()).andExpect(status().isNotFound());
        upload(register(Role.CUSTOMER), product, image()).andExpect(status().isForbidden());
        upload(shop.owner(), product, "%PDF-1.7 documento".getBytes())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));
        upload(shop.owner(), product, "no es una imagen".getBytes())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"));

        // Un documento de identidad vive en el mismo almacen
        Account courier = register(Role.COURIER);
        mvc.perform(put("/api/v1/courier/profile").header("Authorization", courier.bearer())
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "documentNumber", "" + (7_000_000 + System.nanoTime() % 900_000), "vehicleType", "BICYCLE",
                        "plate", "", "acceptsDataProcessing", true)))).andExpect(status().isOk());
        mvc.perform(multipart("/api/v1/courier/documents/IDENTITY")
                .file(new MockMultipartFile("file", "d.png", "image/png", image()))
                .header("Authorization", courier.bearer())).andExpect(status().isOk());
        String documentKey = jdbc.queryForObject(
                "select storage_key from courier_documents where user_id = ?", String.class, courier.id());

        mvc.perform(get("/api/v1/photos/" + documentKey)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/photos/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/photos/cualquier-cosa")).andExpect(status().isNotFound());
    }

    @Test
    void theScheduleOpensAndClosesTheShop() throws Exception {
        Shop shop = openShop("Con horario " + UUID.randomUUID());
        UUID product = addProduct(shop, "Almuerzo", 10000, 4);
        Account customer = register(Role.CUSTOMER);

        hours(shop.owner(), List.of(shift(3, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hours.length()").value(1));
        JsonNode closed = body(store(customer, shop).andReturn());
        assertThat(closed.get("business").get("open").asBoolean()).isFalse();
        assertThat(closed.get("hours").get(0).get("opens").asText()).matches("\\d\\d:\\d\\d");
        mvc.perform(get("/api/v1/catalog/businesses?q=Con horario").header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.items[?(@.id == '" + shop.businessId() + "')].open").value(false));
        place(customer, orderData(shop, product, 1, "PICKUP", 10000))
                .andExpect(jsonPath("$.code").value("OUTSIDE_HOURS"));
        assertThat(jdbc.queryForObject("select stock from products where id = ?", Integer.class, product))
                .isEqualTo(4);

        // Cambiar el horario reemplaza el anterior
        hours(shop.owner(), List.of(shift(-1, 3), shift(30, 1))).andExpect(jsonPath("$.hours.length()").value(2));
        store(customer, shop).andExpect(jsonPath("$.business.open").value(true));
        place(customer, orderData(shop, product, 1, "PICKUP", 10000)).andExpect(status().isCreated());

        hours(shop.owner(), List.of()).andExpect(jsonPath("$.hours.length()").value(0));
        store(customer, shop).andExpect(jsonPath("$.business.open").value(true));
        // El interruptor sigue mandando aunque el horario permita
        setAccepting(shop.owner(), false);
        store(customer, shop).andExpect(jsonPath("$.business.open").value(false));
    }

    @Test
    void anInvalidScheduleIsRejected() throws Exception {
        Shop shop = openShop("Mal horario " + UUID.randomUUID());
        Map<String, Object> morning = Map.of("day", 1, "opens", "08:00", "closes", "14:00");

        hours(shop.owner(), List.of(morning, Map.of("day", 1, "opens", "13:00", "closes", "20:00")))
                .andExpect(jsonPath("$.code").value("INVALID_HOURS"));
        hours(shop.owner(), List.of(Map.of("day", 8, "opens", "08:00", "closes", "14:00")))
                .andExpect(status().isBadRequest());
        hours(shop.owner(), List.of(Map.of("day", 1, "opens", "25:00", "closes", "14:00")))
                .andExpect(status().isBadRequest());
        hours(register(Role.CUSTOMER), List.of(morning)).andExpect(status().isForbidden());
        hours(shop.owner(), List.of(morning)).andExpect(status().isOk())
                .andExpect(jsonPath("$.hours[0].opens").value("08:00"))
                .andExpect(jsonPath("$.hours[0].closes").value("14:00"));
        // 08:00 son 480 minutos, sin importar la zona horaria del servidor
        assertThat(jdbc.queryForObject("select opens from business_hours where business_id = ?",
                Integer.class, shop.businessId())).isEqualTo(480);
    }
}
