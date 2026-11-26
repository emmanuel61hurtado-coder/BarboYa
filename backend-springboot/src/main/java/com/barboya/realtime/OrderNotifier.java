package com.barboya.realtime;

import com.barboya.couriers.CourierPresence;
import com.barboya.couriers.CourierPresenceRepository;
import com.barboya.orders.OrderChanged;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Avisa despues de confirmar el cambio y sin retrasar la respuesta de la API. */
@Component
class OrderNotifier {

    private final EventsSocketHandler socket;
    private final CourierPresenceRepository presence;

    OrderNotifier(EventsSocketHandler socket, CourierPresenceRepository presence) {
        this.socket = socket;
        this.presence = presence;
    }

    @Async
    @TransactionalEventListener
    public void orderChanged(OrderChanged event) {
        Set<UUID> participants = Stream.of(event.customerId(), event.merchantId(), event.courierId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        socket.sendTo(participants, Map.of("type", "order.updated", "orderId", event.orderId()));
        if (event.offersChanged()) {
            Set<UUID> online = presence.findByOnlineTrue().stream()
                    .map(CourierPresence::getCourierId)
                    .collect(Collectors.toSet());
            socket.sendTo(online, Map.of("type", "offers.updated"));
        }
    }
}
