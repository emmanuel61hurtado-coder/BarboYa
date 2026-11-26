package com.barboya.barboya.controller;

import com.barboya.barboya.dto.TripDtos;
import com.barboya.barboya.model.Trip;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.TripService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/trips", "/api/trips"})
public class TripsController {
    private final TripService trips;

    public TripsController(TripService trips) {
        this.trips = trips;
    }

    @PostMapping("/quote")
    public TripDtos.QuoteResponse quote(@RequestBody TripDtos.QuoteRequest req) {
        return trips.quote(req);
    }

    @PostMapping
    public Trip create(@RequestBody TripDtos.CreateTripRequest req) {
        return trips.create(CurrentUser.id(), req);
    }

    @GetMapping
    public List<Trip> mine() {
        return trips.myTrips(CurrentUser.id());
    }

    @GetMapping("/{id}")
    public Trip get(@PathVariable UUID id) {
        return trips.get(id);
    }

    @PostMapping("/{id}/driver-accept")
    public Trip driverAccept(@PathVariable UUID id) {
        return trips.driverAccept(id, CurrentUser.id());
    }

    @PostMapping("/{id}/accept")
    public Trip accept(@PathVariable UUID id) {
        return trips.accept(id);
    }

    @PostMapping("/{id}/verify-pin")
    public Trip verifyPin(@PathVariable UUID id, @RequestBody TripDtos.VerifyPinRequest req) {
        return trips.verifyPin(id, req.pin());
    }

    @PostMapping("/{id}/complete")
    public Trip complete(@PathVariable UUID id) {
        return trips.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public Trip cancel(@PathVariable UUID id, @RequestParam(defaultValue = "customer") String by) {
        return by.equals("driver") ? trips.cancelByDriver(id) : trips.cancelByCustomer(id);
    }
}