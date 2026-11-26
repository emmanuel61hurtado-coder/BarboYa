package com.barboya.realtime;

import com.barboya.auth.AuthSessionRepository;
import com.barboya.security.JwtService;
import com.barboya.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Avisos en vivo. El cliente se identifica con su primer mensaje (el token no va
 * en la URL, que queda en registros) y despues solo recibe "este pedido cambio":
 * los datos los pide por la API, con sus permisos. Las conexiones viven en
 * memoria: con varias instancias hara falta un intermediario como Redis.
 */
@Component
public class EventsSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EventsSocketHandler.class);
    private static final CloseStatus UNAUTHORIZED = new CloseStatus(4401, "No autorizado");
    private static final int MAX_CONNECTIONS_PER_USER = 8;

    private record Connection(WebSocketSession socket, UUID userId, UUID sessionId) {
    }

    private final JwtService jwt;
    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final ObjectMapper json;
    private final Clock clock;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> deadlines = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("events-auth").daemon(true).factory());

    public EventsSocketHandler(JwtService jwt, AuthSessionRepository sessions, UserRepository users,
                               ObjectMapper json, Clock clock) {
        this.jwt = jwt;
        this.sessions = sessions;
        this.users = users;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession socket) {
        socket.setTextMessageSizeLimit(4096);
        // Quien no se identifique pronto se desconecta
        deadlines.put(socket.getId(), timer.schedule(() -> close(socket, UNAUTHORIZED), 10, TimeUnit.SECONDS));
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message) throws IOException {
        if (connections.containsKey(socket.getId())) {
            return;
        }
        var claims = jwt.parse(token(message));
        boolean valid = claims.filter(c -> isActive(c.userId(), c.sessionId())).isPresent();
        if (!valid) {
            close(socket, UNAUTHORIZED);
            return;
        }
        UUID userId = claims.get().userId();
        if (connections.values().stream().filter(c -> c.userId().equals(userId)).count()
                >= MAX_CONNECTIONS_PER_USER) {
            close(socket, CloseStatus.POLICY_VIOLATION);
            return;
        }
        cancelDeadline(socket);
        // Permite enviar desde varios hilos sin mezclar mensajes
        var safe = new ConcurrentWebSocketSessionDecorator(socket, 5_000, 32 * 1024);
        connections.put(socket.getId(), new Connection(safe, userId, claims.get().sessionId()));
        send(safe, Map.of("type", "ready"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) {
        cancelDeadline(socket);
        connections.remove(socket.getId());
    }

    /** Envia un aviso a las conexiones vigentes de esas personas. */
    void sendTo(Set<UUID> recipients, Map<String, ?> payload) {
        for (Connection connection : connections.values()) {
            if (!recipients.contains(connection.userId())) {
                continue;
            }
            // Una sesion cerrada o una cuenta suspendida deja de recibir avisos
            if (!isActive(connection.userId(), connection.sessionId())) {
                close(connection.socket(), UNAUTHORIZED);
                continue;
            }
            send(connection.socket(), payload);
        }
    }

    @PreDestroy
    void shutdown() {
        timer.shutdownNow();
    }

    private String token(TextMessage message) {
        try {
            var node = json.readTree(message.getPayload());
            return "auth".equals(node.path("type").asText()) ? node.path("token").asText() : "";
        } catch (IOException ex) {
            return "";
        }
    }

    private boolean isActive(UUID userId, UUID sessionId) {
        return sessions.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId) && s.isActive(clock.instant()))
                .isPresent()
                && users.findById(userId).filter(u -> !u.isSuspended()).isPresent();
    }

    private void send(WebSocketSession socket, Map<String, ?> payload) {
        try {
            socket.sendMessage(new TextMessage(json.writeValueAsString(payload)));
        } catch (IOException | IllegalStateException ex) {
            log.debug("No se pudo enviar un aviso: {}", ex.getMessage());
            close(socket, CloseStatus.SERVER_ERROR);
        }
    }

    private void cancelDeadline(WebSocketSession socket) {
        ScheduledFuture<?> deadline = deadlines.remove(socket.getId());
        if (deadline != null) {
            deadline.cancel(false);
        }
    }

    private void close(WebSocketSession socket, CloseStatus status) {
        connections.remove(socket.getId());
        try {
            socket.close(status);
        } catch (IOException ex) {
            log.debug("Error al cerrar una conexion: {}", ex.getMessage());
        }
    }
}
