package com.barboya.orders;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourierDeliveryTest extends CommerceFixtures {

    // Comercio en el parque de Barbosa; 0.009 grados de latitud son cerca de 1 km
    private static final double LAT = 5.9333;
    private static final double LNG = -73.6153;
    private static final double ONE_KM = 0.008993;
    private static final AtomicInteger DOCUMENTS = new AtomicInteger(3_000_000);

    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void forgetOtherCouriers() {
        // Cada prueba parte sin pedidos abiertos de las anteriores
        jdbc.update("update orders set status = 'CANCELLED' where fulfillment = 'COURIER' "
                + "and status in ('PLACED', 'PREPARING', 'READY', 'OUT_FOR_DELIVERY')");
    }

    private Shop courierShop() throws Exception {
        Account owner = register(Role.RESTAURANT);
        approve(owner);
        Map<String, Object> data = businessData("Con domiciliarios " + UUID.randomUUID());
        data.put("courierDeliveryEnabled", true);
        data.put("latitude", LAT);
        data.put("longitude", LNG);
        JsonNode saved = body(saveBusiness(owner, data).andExpect(status().isOk()).andReturn());
        setAccepting(owner, true);
        return new Shop(owner, UUID.fromString(saved.get("id").asText()));
    }

    private static byte[] image() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    /** Domiciliario verificado de principio a fin a traves de la API. */
    private Account courier(String vehicle) throws Exception {
        Account courier = register(Role.COURIER);
        Account admin = accountWithRole(Role.ADMIN);
        mvc.perform(put("/api/v1/courier/profile").header("Authorization", courier.bearer())
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "documentNumber", "" + DOCUMENTS.incrementAndGet(), "vehicleType", vehicle,
                        "plate", vehicle.equals("BICYCLE") ? "" : "XYZ98A", "acceptsDataProcessing", true))))
                .andExpect(status().isOk());
        List<String> types = new ArrayList<>(List.of("IDENTITY", "FACE_PHOTO"));
        if (!vehicle.equals("BICYCLE")) {
            types.addAll(List.of("DRIVER_LICENSE", "SOAT"));
        }
        JsonNode view = null;
        for (String type : types) {
            view = body(mvc.perform(multipart("/api/v1/courier/documents/" + type)
                    .file(new MockMultipartFile("file", "d.png", "image/png", image()))
                    .param("expiresOn", LocalDate.now().plusMonths(6).toString())
                    .header("Authorization", courier.bearer())).andExpect(status().isOk()).andReturn());
        }
        for (JsonNode document : view.get("documents")) {
            mvc.perform(postJson("/api/v1/admin/documents/" + document.get("id").asText() + "/review",
                    Map.of("decision", "APPROVED")).header("Authorization", admin.bearer()))
                    .andExpect(status().isOk());
        }
        mvc.perform(patch("/api/v1/admin/users/" + courier.id() + "/status")
                .header("Authorization", admin.bearer()).contentType("application/json")
                .content("{\"status\":\"ACTIVE\",\"reason\":\"Documentos revisados\"}")).andExpect(status().isOk());
        return courier;
    }

    private ResultActions presence(Account courier, boolean online, Double latitude, Double longitude)
            throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("online", online);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        return mvc.perform(put("/api/v1/courier/presence").header("Authorization", courier.bearer())
                .contentType("application/json").content(json.writeValueAsString(data)));
    }

    private Account onlineCourier() throws Exception {
        Account courier = courier("MOTORCYCLE");
        presence(courier, true, LAT, LNG).andExpect(status().isOk());
        return courier;
    }

    private Map<String, Object> courierOrder(Shop shop, UUID product, double kmNorth, int total) {
        Map<String, Object> data = orderData(shop, product, 1, "COURIER", total);
        data.put("deliveryLatitude", LAT + kmNorth * ONE_KM);
        data.put("deliveryLongitude", LNG);
        return data;
    }

    /** Pedido a 2 km ya aceptado por el comercio. */
    private JsonNode preparingOrder(Shop shop, Account customer) throws Exception {
        UUID product = addProduct(shop, "Almuerzo", 10000, null);
        JsonNode order = placed(customer, courierOrder(shop, product, 2, 15000));
        merchantAction(shop, order.get("id").asText(), "accept", Map.of("prepMinutes", 10));
        return order;
    }

    private ResultActions act(Account courier, String path, Map<String, ?> data) throws Exception {
        return mvc.perform(postJson("/api/v1/courier/" + path, data).header("Authorization", courier.bearer()));
    }

    private ResultActions offers(Account courier) throws Exception {
        return mvc.perform(get("/api/v1/courier/offers").header("Authorization", courier.bearer()));
    }

    private String pickupCode(Shop shop) throws Exception {
        return body(mvc.perform(get("/api/v1/merchant/orders").header("Authorization", shop.owner().bearer()))
                .andReturn()).get("items").get(0).get("pickupCode").asText();
    }

    private static String otherThan(String code) {
        return code.equals("0000") ? "1111" : "0000";
    }

    @Test
    void theFeeDependsOnTheDistanceAndHasALimit() throws Exception {
        Shop shop = courierShop();
        Account customer = register(Role.CUSTOMER);
        String quote = "/api/v1/catalog/businesses/" + shop.businessId() + "/delivery-quote?lng=" + LNG + "&lat=";

        mvc.perform(get(quote + (LAT + 2 * ONE_KM)).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.distanceKm").value(2.0))
                .andExpect(jsonPath("$.feeCop").value(5000));
        mvc.perform(get(quote + (LAT + 9 * ONE_KM)).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.code").value("OUT_OF_RANGE"));

        UUID product = addProduct(shop, "Sopa", 10000, 5);
        place(customer, courierOrder(shop, product, 2, 13000)).andExpect(jsonPath("$.code").value("PRICE_CHANGED"));
        place(customer, courierOrder(shop, product, 9, 22000)).andExpect(jsonPath("$.code").value("OUT_OF_RANGE"));
        var withoutPoint = orderData(shop, product, 1, "COURIER", 15000);
        place(customer, withoutPoint).andExpect(jsonPath("$.code").value("LOCATION_REQUIRED"));
        Shop ownStaff = openShop("Sin domiciliarios " + UUID.randomUUID());
        place(customer, courierOrder(ownStaff, addProduct(ownStaff, "Te", 1000, null), 1, 1000))
                .andExpect(jsonPath("$.code").value("FULFILLMENT_UNAVAILABLE"));
    }

    @Test
    void anOrderGoesFromTheShopToTheDoorWithCodeEvidenceAndPin() throws Exception {
        Shop shop = courierShop();
        Account customer = register(Role.CUSTOMER);
        Account courier = onlineCourier();
        JsonNode order = preparingOrder(shop, customer);
        String id = order.get("id").asText();
        String pin = order.get("deliveryPin").asText();

        // Antes de tomarlo no se revela quien es ni donde vive el cliente
        String offer = offers(courier)
                .andExpect(jsonPath("$[0].payAtPickupCop").value(10000))
                .andExpect(jsonPath("$[0].collectCop").value(15000))
                .andExpect(jsonPath("$[0].feeCop").value(5000))
                .andReturn().getResponse().getContentAsString();
        assertThat(offer).doesNotContain("Carrera 7", customer.phone());

        act(courier, "offers/" + id + "/accept", Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.phone").value(customer.phone()))
                .andExpect(jsonPath("$.pickupCode").doesNotExist())
                .andExpect(jsonPath("$.deliveryPin").doesNotExist());
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.courier.plate").value("XYZ98A"))
                .andExpect(jsonPath("$.courier.location.latitude").value(LAT))
                .andExpect(jsonPath("$.pickupCode").doesNotExist());

        String code = pickupCode(shop);
        act(courier, "delivery/" + id + "/pickup", Map.of("code", code)).andExpect(status().isConflict());
        merchantAction(shop, id, "ready", Map.of());
        merchantAction(shop, id, "dispatch", Map.of()).andExpect(status().isConflict());
        merchantAction(shop, id, "deliver", Map.of("pin", pin)).andExpect(status().isConflict());
        act(courier, "delivery/" + id + "/pickup", Map.of("code", otherThan(code)))
                .andExpect(jsonPath("$.code").value("WRONG_CODE"));
        act(courier, "delivery/" + id + "/deliver", Map.of("pin", pin)).andExpect(status().isConflict());
        act(courier, "delivery/" + id + "/pickup", Map.of("code", code))
                .andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));
        merchantAction(shop, id, "cancel", Map.of("reason", "Ya salio"))
                .andExpect(jsonPath("$.code").value("ALREADY_PICKED_UP"));

        byte[] photo = image();
        mvc.perform(multipart("/api/v1/courier/delivery/" + id + "/evidence")
                        .file(new MockMultipartFile("file", "e.png", "image/png", photo))
                        .header("Authorization", courier.bearer()))
                .andExpect(jsonPath("$.hasEvidence").value(true));
        act(courier, "delivery/" + id + "/deliver", Map.of("pin", otherThan(pin)))
                .andExpect(jsonPath("$.code").value("WRONG_PIN"));
        act(courier, "delivery/" + id + "/deliver", Map.of("pin", pin))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                // Terminada la entrega, deja de ver al cliente
                .andExpect(jsonPath("$.customer").doesNotExist())
                .andExpect(jsonPath("$.deliveryAddress").doesNotExist());

        mvc.perform(get("/api/v1/courier/delivery").header("Authorization", courier.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/courier/earnings").header("Authorization", courier.bearer()))
                // Tarifa de 5.000 menos el 10 % de comision
                .andExpect(jsonPath("$.todayCop").value(4500))
                .andExpect(jsonPath("$.last30DaysCop").value(4500));
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.courier.location").doesNotExist())
                .andExpect(jsonPath("$.courier.phone").doesNotExist());

        for (String path : new String[]{"/api/v1/orders/", "/api/v1/merchant/orders/", "/api/v1/courier/delivery/"}) {
            Account actor = path.contains("merchant") ? shop.owner() : path.contains("courier") ? courier : customer;
            mvc.perform(get(path + id + "/evidence").header("Authorization", actor.bearer()))
                    .andExpect(content().bytes(photo));
        }
        mvc.perform(get("/api/v1/orders/" + id + "/evidence")
                .header("Authorization", register(Role.CUSTOMER).bearer())).andExpect(status().isNotFound());
    }

    @Test
    void onlyVerifiedNearbyAndOnlineCouriersSeeOrders() throws Exception {
        Shop shop = courierShop();
        preparingOrder(shop, register(Role.CUSTOMER));

        Account unverified = register(Role.COURIER);
        presence(unverified, true, LAT, LNG).andExpect(jsonPath("$.code").value("COURIER_NOT_OPERABLE"));
        offers(unverified).andExpect(status().isForbidden());

        Account courier = courier("MOTORCYCLE");
        offers(courier).andExpect(jsonPath("$.code").value("NOT_AVAILABLE"));
        presence(courier, true, null, null).andExpect(jsonPath("$.code").value("LOCATION_REQUIRED"));
        presence(courier, true, LAT + 7 * ONE_KM, LNG);
        offers(courier).andExpect(jsonPath("$.length()").value(0));
        presence(courier, true, LAT, LNG);
        offers(courier).andExpect(jsonPath("$.length()").value(1));

        clock.advance(Duration.ofMinutes(3));
        Account refreshed = account(login(courier.email(), PASSWORD, "a"));
        offers(refreshed).andExpect(jsonPath("$.code").value("NOT_AVAILABLE"));
        presence(refreshed, false, null, null);
        assertThat(jdbc.queryForObject("select latitude from courier_presence where courier_id = ?",
                Double.class, courier.id())).isNull();
    }

    @Test
    void aBicycleIsNotOfferedLongRoutes() throws Exception {
        Shop shop = courierShop();
        UUID product = addProduct(shop, "Paquete", 10000, null);
        Account customer = register(Role.CUSTOMER);
        String near = placed(customer, courierOrder(shop, product, 2, 15000)).get("id").asText();
        String far = placed(customer, courierOrder(shop, product, 6, 19000)).get("id").asText();
        merchantAction(shop, near, "accept", Map.of("prepMinutes", 5));
        merchantAction(shop, far, "accept", Map.of("prepMinutes", 5));
        Account cyclist = courier("BICYCLE");
        presence(cyclist, true, LAT, LNG);

        offers(cyclist).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(near));
        act(cyclist, "offers/" + far + "/accept", Map.of())
                .andExpect(jsonPath("$.code").value("OFFER_NOT_ELIGIBLE"));
    }

    @Test
    void oneCourierPerOrderAndOneOrderPerCourier() throws Exception {
        Shop shop = courierShop();
        String id = preparingOrder(shop, register(Role.CUSTOMER)).get("id").asText();
        List<Account> couriers = List.of(onlineCourier(), onlineCourier(), onlineCourier());
        CountDownLatch start = new CountDownLatch(1);
        Account winner = null;

        try (var pool = Executors.newFixedThreadPool(3)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (Account courier : couriers) {
                results.add(pool.submit(() -> {
                    start.await();
                    return act(courier, "offers/" + id + "/accept", Map.of()).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                int code = results.get(i).get();
                statuses.add(code);
                if (code == 200) {
                    winner = couriers.get(i);
                }
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409, 409);
        }

        String second = preparingOrder(shop, register(Role.CUSTOMER)).get("id").asText();
        offers(winner).andExpect(jsonPath("$.length()").value(0));
        act(winner, "offers/" + second + "/accept", Map.of())
                .andExpect(jsonPath("$.code").value("ALREADY_DELIVERING"));
    }

    @Test
    void anOrderCanChangeCourierBeforePickup() throws Exception {
        Shop shop = courierShop();
        String id = preparingOrder(shop, register(Role.CUSTOMER)).get("id").asText();
        Account first = onlineCourier();
        Account second = onlineCourier();
        act(first, "offers/" + id + "/accept", Map.of());
        String firstCode = pickupCode(shop);

        act(second, "delivery/" + id + "/release", Map.of("reason", "No es mio")).andExpect(status().isNotFound());
        act(first, "delivery/" + id + "/release", Map.of("reason", "Se pincho la llanta"))
                .andExpect(status().isNoContent());
        offers(second).andExpect(jsonPath("$.length()").value(1));
        act(second, "offers/" + id + "/accept", Map.of()).andExpect(status().isOk());
        merchantAction(shop, id, "unassign", Map.of()).andExpect(status().isOk());
        merchantAction(shop, id, "unassign", Map.of()).andExpect(jsonPath("$.code").value("NO_COURIER"));
        act(second, "delivery/" + id + "/pickup", Map.of("code", firstCode)).andExpect(status().isNotFound());
    }

    @Test
    void aDeliveryThatCannotBeCompletedIsRecordedAsFailed() throws Exception {
        Shop shop = courierShop();
        Account customer = register(Role.CUSTOMER);
        Account courier = onlineCourier();
        String id = preparingOrder(shop, customer).get("id").asText();
        act(courier, "offers/" + id + "/accept", Map.of());
        String code = pickupCode(shop);
        merchantAction(shop, id, "ready", Map.of());
        act(courier, "delivery/" + id + "/fail", Map.of("reason", "Nadie abre")).andExpect(status().isConflict());
        act(courier, "delivery/" + id + "/pickup", Map.of("code", code));

        act(courier, "delivery/" + id + "/release", Map.of("reason", "Ya no puedo"))
                .andExpect(status().isConflict());
        act(courier, "delivery/" + id + "/fail", Map.of("reason", "Nadie atendio en la direccion"))
                .andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(get("/api/v1/orders/" + id).header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.statusReason").value("Nadie atendio en la direccion"))
                .andExpect(jsonPath("$.deliveryPin").doesNotExist());
        mvc.perform(get("/api/v1/courier/earnings").header("Authorization", courier.bearer()))
                .andExpect(jsonPath("$.todayCop").value(0));
    }

    @Test
    void anExpiredDocumentStopsNewAssignments() throws Exception {
        Shop shop = courierShop();
        Account courier = onlineCourier();
        clock.advance(Duration.ofDays(200));
        Account later = account(login(courier.email(), PASSWORD, "a"));

        presence(later, true, LAT, LNG).andExpect(jsonPath("$.code").value("COURIER_NOT_OPERABLE"));
        offers(later).andExpect(status().isForbidden());
        act(later, "offers/" + UUID.randomUUID() + "/accept", Map.of()).andExpect(status().isForbidden());
    }

    @Test
    void otherRolesCannotActAsCouriers() throws Exception {
        for (Role role : new Role[]{Role.CUSTOMER, Role.RESTAURANT, Role.MOTO_DRIVER}) {
            Account actor = register(role);
            offers(actor).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/courier/offers/" + UUID.randomUUID() + "/accept")
                    .header("Authorization", actor.bearer())).andExpect(status().isForbidden());
        }
    }
}
