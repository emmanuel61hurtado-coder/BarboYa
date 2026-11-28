package com.barboya.security;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ventana fija de un minuto por clave, en memoria. Vale para una sola instancia;
 * con varias habra que llevar el contador a Redis.
 */
public class RateLimiter {

    private record Window(long minute, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final Clock clock;

    public RateLimiter(int limit, Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    public boolean allow(String key) {
        long minute = clock.millis() / 60_000;
        if (windows.size() > 10_000) {
            windows.values().removeIf(w -> w.minute() < minute);
        }
        Window updated = windows.merge(key, new Window(minute, 1),
                (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);
        return updated.count() <= limit;
    }
}
