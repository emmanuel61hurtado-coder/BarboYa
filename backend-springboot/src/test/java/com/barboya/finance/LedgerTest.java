package com.barboya.finance;

import com.barboya.config.FinanceProperties;
import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerTest extends CommerceFixtures {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transaction;

    /** Entrega completa de un pedido recogido en el local. */
    private String deliver(Shop shop, int price) throws Exception {
        UUID product = addProduct(shop, "Producto " + UUID.randomUUID(), price, null);
        JsonNode order = placed(register(Role.CUSTOMER), orderData(shop, product, 1, "PICKUP", price));
        String id = order.get("id").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5));
        merchantAction(shop, id, "ready", Map.of());
        merchantAction(shop, id, "deliver", Map.of("pin", order.get("deliveryPin").asText()))
                .andExpect(status().isOk());
        return id;
    }

    private ResultActions statement(Account account) throws Exception {
        return mvc.perform(get("/api/v1/finance/statement").header("Authorization", account.bearer()));
    }

    private ResultActions pay(Account admin, Account party, int amount, String reference) throws Exception {
        return mvc.perform(postJson("/api/v1/admin/finance/accounts/" + party.id() + "/payments",
                Map.of("amountCop", amount, "method", "BANK_TRANSFER", "reference", reference))
                .header("Authorization", admin.bearer()));
    }

    private ResultActions adjust(Account actor, Account party, int amount) throws Exception {
        return mvc.perform(postJson("/api/v1/admin/finance/accounts/" + party.id() + "/adjustments",
                Map.of("amountCop", amount, "reason", "Acuerdo comercial documentado"))
                .header("Authorization", actor.bearer()));
    }

    private ResultActions reverse(Account actor, String entry) throws Exception {
        return mvc.perform(postJson("/api/v1/admin/finance/entries/" + entry + "/reversal",
                Map.of("reason", "Se registro por error"))
                .header("Authorization", actor.bearer()));
    }

    private static String reference() {
        return "TRX-" + UUID.randomUUID().toString().substring(0, 12);
    }

    @ParameterizedTest
    @CsvSource({"20000, 2000", "4995, 500", "4994, 499", "100, 10", "4, 0", "10000000, 1000000"})
    void commissionsRoundToTheNearestPeso(int amount, int commission) {
        assertThat(new FinanceProperties(1000, 1000, 50000).merchantCommission(amount)).isEqualTo(commission);
    }

    @Test
    void commissionIsChargedOnlyWhenTheOrderIsDelivered() throws Exception {
        Shop shop = openShop("Comisiones " + UUID.randomUUID());
        UUID product = addProduct(shop, "Plato", 20000, null);
        Account customer = register(Role.CUSTOMER);
        var data = orderData(shop, product, 1, "DELIVERY", 23000);
        String cancelled = placed(customer, data).get("id").asText();
        merchantAction(shop, cancelled, "reject", Map.of("reason", "No hay gas"));
        statement(shop.owner()).andExpect(jsonPath("$.balanceCop").value(0));

        JsonNode order = placed(customer, data);
        // El cliente paga lo mismo y no ve la comision
        assertThat(order.get("totalCop").asInt()).isEqualTo(23000);
        assertThat(order.get("commissionCop").isNull()).isTrue();
        String id = order.get("id").asText();
        merchantAction(shop, id, "accept", Map.of("prepMinutes", 5))
                .andExpect(jsonPath("$.commissionCop").value(2000));
        merchantAction(shop, id, "ready", Map.of());
        merchantAction(shop, id, "dispatch", Map.of());
        statement(shop.owner()).andExpect(jsonPath("$.balanceCop").value(0));
        merchantAction(shop, id, "deliver", Map.of("pin", order.get("deliveryPin").asText()));
        merchantAction(shop, id, "deliver", Map.of("pin", order.get("deliveryPin").asText()))
                .andExpect(status().isConflict());

        // 10 % de los productos; el domicilio del comercio no paga comision
        statement(shop.owner())
                .andExpect(jsonPath("$.balanceCop").value(2000))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.movements[0].effectCop").value(2000))
                .andExpect(jsonPath("$.commissionBps").value(1000));
    }

    @Test
    void paymentsReduceTheDebtOnceAndNeverBelowZero() throws Exception {
        Shop shop = openShop("Pagos " + UUID.randomUUID());
        deliver(shop, 50000);
        Account admin = accountWithRole(Role.ADMIN);
        String reference = reference();

        pay(admin, shop.owner(), 5001, reference).andExpect(jsonPath("$.code").value("AMOUNT_ABOVE_BALANCE"));
        pay(admin, shop.owner(), 0, reference).andExpect(status().isBadRequest());
        pay(admin, shop.owner(), -100, reference).andExpect(status().isBadRequest());
        pay(admin, shop.owner(), 3000, reference).andExpect(jsonPath("$.balanceCop").value(2000));
        pay(admin, shop.owner(), 1000, reference.toLowerCase())
                .andExpect(jsonPath("$.code").value("REFERENCE_IN_USE"));
        statement(shop.owner())
                .andExpect(jsonPath("$.balanceCop").value(2000))
                .andExpect(jsonPath("$.movements[0].reference").value(reference));
        pay(admin, register(Role.CUSTOMER), 100, reference()).andExpect(status().isNotFound());
    }

    @Test
    void theSameTransferCannotBeCreditedTwiceAtOnce() throws Exception {
        Shop first = openShop("Doble " + UUID.randomUUID());
        Shop second = openShop("Doble " + UUID.randomUUID());
        deliver(first, 50000);
        deliver(second, 50000);
        Account admin = accountWithRole(Role.ADMIN);
        String reference = reference();
        CountDownLatch start = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (Shop shop : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    start.await();
                    return pay(admin, shop.owner(), 5000, reference).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            assertThat(List.of(results.get(0).get(), results.get(1).get()))
                    .containsExactlyInAnyOrder(201, 409);
        }
    }

    @Test
    void mistakesAreReversedNeverErased() throws Exception {
        Shop shop = openShop("Reversos " + UUID.randomUUID());
        deliver(shop, 50000);
        Account admin = accountWithRole(Role.ADMIN);
        Account boss = accountWithRole(Role.SUPERADMIN);
        String reference = reference();
        String payment = body(pay(admin, shop.owner(), 5000, reference).andReturn())
                .get("movements").get(0).get("id").asText();

        reverse(admin, payment).andExpect(status().isForbidden());
        reverse(boss, payment)
                .andExpect(jsonPath("$.balanceCop").value(5000))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.movements[0].kind").value("REVERSAL"))
                .andExpect(jsonPath("$.movements[1].reversed").value(true));
        reverse(boss, payment).andExpect(jsonPath("$.code").value("ALREADY_REVERSED"));
        // Reversado el pago, la referencia se puede registrar bien
        pay(admin, shop.owner(), 4000, reference).andExpect(jsonPath("$.balanceCop").value(1000));
        String commission = jdbc.queryForObject(
                "select id::text from ledger_entries where party_id = ? and kind = 'COMMISSION'",
                String.class, shop.owner().id());
        reverse(boss, commission).andExpect(jsonPath("$.code").value("NOT_REVERSIBLE"));

        adjust(admin, shop.owner(), 1000).andExpect(status().isForbidden());
        adjust(boss, shop.owner(), 1000).andExpect(jsonPath("$.balanceCop").value(0));
    }

    @Test
    void aDebtAboveTheLimitStopsNewOrdersUntilPaid() throws Exception {
        Shop shop = openShop("Tope " + UUID.randomUUID());
        UUID product = addProduct(shop, "Algo", 1000, null);
        Account customer = register(Role.CUSTOMER);
        deliver(shop, 500000);
        // Justo en el tope todavia puede recibir pedidos
        place(customer, orderData(shop, product, 1, "PICKUP", 1000)).andExpect(status().isCreated());
        deliver(shop, 1000);

        place(customer, orderData(shop, product, 1, "PICKUP", 1000))
                .andExpect(jsonPath("$.code").value("BUSINESS_CLOSED"));
        mvc.perform(get("/api/v1/catalog/businesses/" + shop.businessId())
                        .header("Authorization", customer.bearer()))
                .andExpect(jsonPath("$.business.open").value(false));
        setAccepting(shop.owner(), false).andExpect(status().isOk());
        setAccepting(shop.owner(), true).andExpect(jsonPath("$.code").value("DEBT_LIMIT"));
        statement(shop.owner()).andExpect(jsonPath("$.blocked").value(true));

        pay(accountWithRole(Role.ADMIN), shop.owner(), 100, reference())
                .andExpect(jsonPath("$.blocked").value(false));
        setAccepting(shop.owner(), true).andExpect(status().isOk());
        place(customer, orderData(shop, product, 1, "PICKUP", 1000)).andExpect(status().isCreated());
    }

    @Test
    void theSummaryMatchesTheBooks() throws Exception {
        Shop shop = openShop("Resumen " + UUID.randomUUID());
        deliver(shop, 30000);
        Account admin = accountWithRole(Role.ADMIN);
        pay(admin, shop.owner(), 1000, reference());

        JsonNode summary = body(mvc.perform(get("/api/v1/admin/finance/summary")
                .header("Authorization", admin.bearer())).andExpect(status().isOk()).andReturn());

        assertThat(summary.get("today").get("commissionsCop").asLong()).isPositive();
        // Todo lo causado esta por cobrar, cobrado o perdonado
        Long revenue = jdbc.queryForObject("select sum(credit_cop - debit_cop) from ledger_lines "
                + "where account = 'COMMISSION_REVENUE'", Long.class);
        assertThat(summary.get("outstandingCop").asLong() + summary.get("collectedCop").asLong()
                + summary.get("waivedCop").asLong()).isEqualTo(revenue);
        assertThat(jdbc.queryForObject("select sum(debit_cop) - sum(credit_cop) from ledger_lines", Long.class))
                .isZero();
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER", "MOTO_DRIVER"})
    void accountsAreOnlyVisibleToTheirOwnersAndStaff(Role role) throws Exception {
        Account actor = register(role);

        statement(actor).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/finance/summary").header("Authorization", actor.bearer()))
                .andExpect(status().isForbidden());
        pay(actor, actor, 100, reference()).andExpect(status().isForbidden());
    }

    @Test
    void theDatabaseRejectsUnbalancedOrRewrittenEntries() throws Exception {
        Shop shop = openShop("Libro " + UUID.randomUUID());
        deliver(shop, 10000);
        Map<String, Object> ids = new HashMap<>();
        ids.put("party", shop.owner().id());

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            UUID entry = UUID.randomUUID();
            jdbc.update("insert into ledger_entries (id, kind, party_id, amount_cop, created_at) "
                    + "values (?, 'ADJUSTMENT', ?, 10, now())", entry, shop.owner().id());
            jdbc.update("insert into ledger_lines (id, entry_id, account, debit_cop, credit_cop) "
                    + "values (?, ?, 'WAIVED', 10, 0)", UUID.randomUUID(), entry);
            jdbc.update("insert into ledger_lines (id, entry_id, account, party_id, debit_cop, credit_cop) "
                    + "values (?, ?, 'RECEIVABLE', ?, 0, 9)", UUID.randomUUID(), entry, shop.owner().id());
        })).isInstanceOfAny(DataAccessException.class, TransactionException.class)
                .hasStackTraceContaining("descuadrado");

        for (String statement : List.of("update ledger_lines set debit_cop = 1", "delete from ledger_entries")) {
            assertThatThrownBy(() -> jdbc.execute(statement)).hasMessageContaining("solo insercion");
        }
    }
}
