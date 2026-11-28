package com.barboya.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "barboya.orders.expiry-job-enabled", havingValue = "true", matchIfMissing = true)
class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderExpiryService expiry;

    OrderExpiryJob(OrderExpiryService expiry) {
        this.expiry = expiry;
    }

    @Scheduled(fixedDelayString = "${barboya.orders.expiry-interval:PT30S}")
    void run() {
        for (UUID id : expiry.overdue()) {
            try {
                expiry.expire(id);
            } catch (RuntimeException ex) {
                // Un pedido con problemas no detiene a los demas
                log.error("No se pudo vencer el pedido {}", id, ex);
            }
        }
    }
}
