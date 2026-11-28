package com.barboya.barboya.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/webhooks", "/api/webhooks"})
public class WebhooksController {
    @PostMapping("/**")
    public Map<String, String> receive(@RequestBody(required = false) Map<String, Object> body) {
        return Map.of("status", "received");
    }
}