package com.barboya.realtime;

import com.barboya.orders.OrderChanged;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Set;

/** Avisa despues de confirmar el cambio y sin retrasar la respuesta de la API. */
@Component
class OrderNotifier {

    private final EventsSocketHandler socket;

    OrderNotifier(EventsSocketHandler socket) {
        this.socket = socket;
    }

    @Async
    @TransactionalEventListener
    public void orderChanged(OrderChanged event) {
        socket.sendTo(Set.of(event.customerId(), event.merchantId()),
                Map.of("type", "order.updated", "orderId", event.orderId()));
    }
}
