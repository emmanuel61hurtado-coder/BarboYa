package com.barboya.barboya.dto;

public final class WebhooksDtos {
    private WebhooksDtos() {}

    public record GenericWebhook(java.util.Map<String, Object> payload) {}
}