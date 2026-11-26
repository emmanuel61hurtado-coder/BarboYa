package com.barboya.realtime;

import com.barboya.support.CommerceFixtures;
import com.barboya.users.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeTest extends CommerceFixtures {

    @LocalServerPort private int port;

    private static final class Listener extends TextWebSocketHandler implements AutoCloseable {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        WebSocketSession socket;

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.add(message.getPayload());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            messages.add("closed:" + status.getCode());
        }

        String next() throws InterruptedException {
            return messages.poll(5, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }

    private Listener connect(String firstMessage, String origin) throws Exception {
        Listener listener = new Listener();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (origin != null) {
            headers.setOrigin(origin);
        }
        listener.socket = new StandardWebSocketClient()
                .execute(listener, headers, URI.create("ws://localhost:" + port + "/ws/v1/events"))
                .get(5, TimeUnit.SECONDS);
        listener.socket.sendMessage(new TextMessage(firstMessage));
        return listener;
    }

    private Listener signedIn(Account account) throws Exception {
        Listener listener = connect("{\"type\":\"auth\",\"token\":\"" + account.accessToken() + "\"}", null);
        assertThat(listener.next()).isEqualTo("{\"type\":\"ready\"}");
        return listener;
    }

    @Test
    void onlyTheParticipantsAreToldAboutAnOrder() throws Exception {
        Shop shop = openShop("En vivo");
        UUID product = addProduct(shop, "Tinto", 1500, null);
        Account customer = register(Role.CUSTOMER);

        try (Listener merchant = signedIn(shop.owner());
             Listener buyer = signedIn(customer);
             Listener stranger = signedIn(register(Role.CUSTOMER))) {
            String id = placed(customer, orderData(shop, product, 1, "PICKUP", 1500)).get("id").asText();

            assertThat(merchant.next()).contains("order.updated", id);
            // El aviso no lleva datos del pedido
            assertThat(buyer.next()).contains("order.updated", id).doesNotContain("Pin", "1500");
            assertThat(stranger.messages.poll(700, TimeUnit.MILLISECONDS)).isNull();
        }
    }

    @Test
    void aBadTokenIsDisconnected() throws Exception {
        try (Listener listener = connect("{\"type\":\"auth\",\"token\":\"falso\"}", null)) {
            assertThat(listener.next()).isEqualTo("closed:4401");
        }
        try (Listener listener = connect("no es json", null)) {
            assertThat(listener.next()).isEqualTo("closed:4401");
        }
    }

    @Test
    void aClosedSessionStopsReceivingNotices() throws Exception {
        Shop shop = openShop("Sesion cerrada");
        UUID product = addProduct(shop, "Pandebono", 1200, null);
        Account customer = register(Role.CUSTOMER);

        try (Listener buyer = signedIn(shop.owner())) {
            mvc.perform(post("/api/v1/auth/logout").cookie(shop.owner().refresh()));
            placed(customer, orderData(shop, product, 1, "PICKUP", 1200));

            assertThat(buyer.next()).isEqualTo("closed:4401");
        }
    }

    @Test
    void otherWebsitesCannotConnect() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> connect("{}", "https://sitio-ajeno.example"))
                .hasMessageContaining("403");
    }
}
