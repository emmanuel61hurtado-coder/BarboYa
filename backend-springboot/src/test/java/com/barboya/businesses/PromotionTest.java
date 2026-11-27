package com.barboya.businesses;

import com.barboya.common.Colombia;
import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PromotionTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;

    private LocalDate today() {
        return Colombia.today(clock);
    }

    private ResultActions promote(Account owner, UUID product, int price, LocalDate from, LocalDate to)
            throws Exception {
        return mvc.perform(put("/api/v1/merchant/products/" + product + "/promotion")
                .header("Authorization", owner.bearer()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("priceCop", price, "startsOn", from.toString(),
                        "endsOn", to.toString()))));
    }

    private ResultActions reprice(Account owner, UUID product, int price) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Bandeja");
        data.put("category", "General");
        data.put("priceCop", price);
        data.put("available", true);
        return mvc.perform(put("/api/v1/merchant/products/" + product).header("Authorization", owner.bearer())
                .contentType("application/json").content(json.writeValueAsString(data)));
    }

    private ResultActions menu(Account customer, Shop shop) throws Exception {
        return mvc.perform(get("/api/v1/catalog/businesses/" + shop.businessId())
                .header("Authorization", customer.bearer()));
    }

    @Test
    void theDiscountIsChargedOnlyBetweenItsDates() throws Exception {
        Shop shop = openShop("Rebajas " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bandeja", 20000, null);
        Account customer = register(Role.CUSTOMER);

        promote(shop.owner(), product, 15000, today(), today().plusDays(2))
                .andExpect(jsonPath("$.priceCop").value(20000))
                .andExpect(jsonPath("$.promotion.priceCop").value(15000))
                .andExpect(jsonPath("$.promotion.active").value(true));
        // La fecha se guarda tal como se escribio, sin correrse por la zona horaria
        assertThat(jdbc.queryForObject("select promo_ends_on::text from products where id = ?", String.class,
                product)).isEqualTo(today().plusDays(2).toString());
        menu(customer, shop)
                .andExpect(jsonPath("$.products[0].priceCop").value(15000))
                .andExpect(jsonPath("$.products[0].regularPriceCop").value(20000))
                .andExpect(jsonPath("$.products[0].promoEndsOn").value(today().plusDays(2).toString()));
        place(customer, orderData(shop, product, 2, "PICKUP", 40000))
                .andExpect(jsonPath("$.code").value("PRICE_CHANGED"));
        place(customer, orderData(shop, product, 2, "PICKUP", 30000))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].unitPriceCop").value(15000))
                .andExpect(jsonPath("$.subtotalCop").value(30000));

        clock.advance(Duration.ofDays(3));
        Account later = account(login(customer.email(), PASSWORD, "pruebas"));
        menu(later, shop)
                .andExpect(jsonPath("$.products[0].priceCop").value(20000))
                .andExpect(jsonPath("$.products[0].regularPriceCop").doesNotExist());
        place(later, orderData(shop, product, 1, "PICKUP", 15000))
                .andExpect(jsonPath("$.code").value("PRICE_CHANGED"));
        place(later, orderData(shop, product, 1, "PICKUP", 20000)).andExpect(status().isCreated());
    }

    @Test
    void aFutureDiscountWaitsForItsDay() throws Exception {
        Shop shop = openShop("Pronto " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bandeja", 20000, null);
        Account customer = register(Role.CUSTOMER);

        promote(shop.owner(), product, 12000, today().plusDays(1), today().plusDays(1))
                .andExpect(jsonPath("$.promotion.active").value(false));
        menu(customer, shop).andExpect(jsonPath("$.products[0].priceCop").value(20000));

        clock.advance(Duration.ofDays(1));
        menu(account(login(customer.email(), PASSWORD, "pruebas")), shop)
                .andExpect(jsonPath("$.products[0].priceCop").value(12000));
    }

    @Test
    void invalidDiscountsAndConflictingPricesAreRejected() throws Exception {
        Shop shop = openShop("Reglas " + UUID.randomUUID());
        Shop other = openShop("Intruso " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bandeja", 20000, null);
        LocalDate today = today();

        for (ResultActions invalid : new ResultActions[]{
                promote(shop.owner(), product, 20000, today, today),
                promote(shop.owner(), product, 15000, today.plusDays(2), today.plusDays(1)),
                promote(shop.owner(), product, 15000, today.minusDays(5), today.minusDays(1)),
                promote(shop.owner(), product, 15000, today, today.plusDays(91))}) {
            invalid.andExpect(jsonPath("$.code").value("INVALID_PROMOTION"));
        }
        promote(shop.owner(), product, 50, today, today).andExpect(status().isBadRequest());
        promote(other.owner(), product, 15000, today, today).andExpect(status().isNotFound());
        promote(register(Role.CUSTOMER), product, 15000, today, today).andExpect(status().isForbidden());

        promote(shop.owner(), product, 15000, today, today).andExpect(status().isOk());
        reprice(shop.owner(), product, 15000).andExpect(jsonPath("$.code").value("PROMOTION_CONFLICT"));
        reprice(shop.owner(), product, 18000).andExpect(jsonPath("$.promotion.priceCop").value(15000));
    }

    private ResultActions remove(Account owner, UUID product) throws Exception {
        return mvc.perform(delete("/api/v1/merchant/products/" + product + "/promotion")
                .header("Authorization", owner.bearer()));
    }

    @Test
    void anAnnouncedDiscountMustBeHonouredUntilItsLastDay() throws Exception {
        Shop shop = openShop("Obliga " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bandeja", 20000, null);
        LocalDate today = today();
        promote(shop.owner(), product, 15000, today, today.plusDays(3)).andExpect(status().isOk());

        for (ResultActions worse : new ResultActions[]{
                remove(shop.owner(), product),
                promote(shop.owner(), product, 16000, today, today.plusDays(3)),
                promote(shop.owner(), product, 15000, today, today.plusDays(2)),
                promote(shop.owner(), product, 15000, today.plusDays(1), today.plusDays(3)),
                reprice(shop.owner(), product, 25000)}) {
            worse.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PROMOTION_BINDING"));
        }
        // Mejorarla si se puede
        promote(shop.owner(), product, 14000, today, today.plusDays(5))
                .andExpect(jsonPath("$.promotion.priceCop").value(14000));
        reprice(shop.owner(), product, 19000).andExpect(status().isOk());
        menu(register(Role.CUSTOMER), shop)
                .andExpect(jsonPath("$.products[0].priceCop").value(14000))
                .andExpect(jsonPath("$.products[0].promoWhileStockLasts").value(false));

        clock.advance(Duration.ofDays(6));
        Account owner = account(login(shop.owner().email(), PASSWORD, "pruebas"));
        remove(owner, product).andExpect(jsonPath("$.promotion").doesNotExist());
    }

    @Test
    void aDiscountThatHasNotStartedCanStillBeWithdrawn() throws Exception {
        Shop shop = openShop("Futura " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bandeja", 20000, 3);
        promote(shop.owner(), product, 15000, today().plusDays(2), today().plusDays(4)).andExpect(status().isOk());

        remove(shop.owner(), product).andExpect(jsonPath("$.promotion").doesNotExist());

        promote(shop.owner(), product, 15000, today(), today()).andExpect(status().isOk());
        menu(register(Role.CUSTOMER), shop)
                .andExpect(jsonPath("$.products[0].promoWhileStockLasts").value(true));
    }

    @Test
    void anExpiredDiscountDoesNotBlockANewPrice() throws Exception {
        Shop shop = openShop("Vencida " + UUID.randomUUID());
        UUID product = addProduct(shop, "Bandeja", 20000, null);
        promote(shop.owner(), product, 15000, today(), today()).andExpect(status().isOk());

        clock.advance(Duration.ofDays(1));
        Account owner = account(login(shop.owner().email(), PASSWORD, "pruebas"));
        reprice(owner, product, 10000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promotion").doesNotExist());
    }
}
