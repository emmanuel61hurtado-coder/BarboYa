package com.barboya.audit;

import com.barboya.common.ClientInfo;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository events;
    private final Clock clock;

    public AuditService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    /** Se guarda dentro de la transaccion de quien llama: accion y registro van juntos. */
    public void record(AuditAction action, UUID actorId, UUID targetId, String detail, ClientInfo client) {
        String text = detail == null || detail.length() <= 500 ? detail : detail.substring(0, 500);
        events.save(new AuditEvent(actorId, targetId, action, text, client.ip(), client.userAgent(), clock.instant()));
    }
}
