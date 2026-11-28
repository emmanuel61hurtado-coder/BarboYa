package com.barboya.realtime;

import com.barboya.config.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class EventsSocketConfig implements WebSocketConfigurer {

    private final EventsSocketHandler handler;
    private final AppProperties properties;

    public EventsSocketConfig(EventsSocketHandler handler, AppProperties properties) {
        this.handler = handler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Sin origenes configurados solo se admite el mismo sitio
        registry.addHandler(handler, "/ws/v1/events")
                .setAllowedOrigins(properties.security().corsAllowedOrigins().stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty() && !origin.contains("*"))
                        .toArray(String[]::new));
    }
}
