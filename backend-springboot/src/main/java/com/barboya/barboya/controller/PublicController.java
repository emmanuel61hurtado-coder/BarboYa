package com.barboya.barboya.controller;

import com.barboya.barboya.model.Trip;
import com.barboya.barboya.repository.TripRepository;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/public", "/api/public"})
public class PublicController {
    private final TripRepository trips;

    public PublicController(TripRepository trips) {
        this.trips = trips;
    }

    @GetMapping("/trips/share/{token}")
    public Trip share(@PathVariable String token) {
        return trips.findAll().stream()
                .filter(t -> token.equals(t.getShareToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No encontrado"));
    }
}