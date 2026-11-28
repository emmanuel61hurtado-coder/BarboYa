package com.barboya.barboya.service;

import com.barboya.barboya.dto.TripDtos;
import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.Trip;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.TripRepository;
import com.barboya.barboya.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {
    private final TripRepository trips;
    private final UserRepository users;
    private final FaresService fares;

    public TripService(TripRepository trips, UserRepository users, FaresService fares) {
        this.trips = trips;
        this.users = users;
        this.fares = fares;
    }

    public TripDtos.QuoteResponse quote(TripDtos.QuoteRequest req) {
        double km = fares.distanceKm(req.originLat(), req.originLng(), req.destLat(), req.destLng());
        int fare = fares.suggestedFare(req.originLat(), req.originLng(), req.destLat(), req.destLng());
        return new TripDtos.QuoteResponse(fare, km);
    }

    public Trip create(UUID customerId, TripDtos.CreateTripRequest req) {
        User customer = users.findById(customerId).orElseThrow();
        Trip t = new Trip();
        t.setCustomer(customer);
        t.setOriginLat(req.originLat());
        t.setOriginLng(req.originLng());
        t.setOriginAddress(req.originAddress());
        t.setDestLat(req.destLat());
        t.setDestLng(req.destLng());
        t.setDestAddress(req.destAddress());
        t.setServiceType(req.serviceType() != null ? req.serviceType() : Enums.ServiceType.passenger);
        t.setPaymentMethod(req.paymentMethod() != null ? req.paymentMethod() : Enums.PaymentMethod.cash);
        t.setSuggestedFareCop(fares.suggestedFare(req.originLat(), req.originLng(), req.destLat(), req.destLng()));
        t.setAgreedFareCop(req.agreedFareCop() > 0 ? req.agreedFareCop() : t.getSuggestedFareCop());
        t.setDistanceKm(fares.distanceKm(req.originLat(), req.originLng(), req.destLat(), req.destLng()));
        t.setPin((int) (Math.random() * 9000) + 1000);
        t.setShareToken(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        t.setStatus(Enums.TripStatus.requested);
        return trips.save(t);
    }

    public Trip get(UUID id) {
        return trips.findById(id).orElseThrow(() -> new RuntimeException("Viaje no encontrado"));
    }

    public List<Trip> myTrips(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        // customer or driver
        List<Trip> asCustomer = trips.findByCustomer(u);
        List<Trip> asDriver = trips.findByDriver(u);
        asCustomer.addAll(asDriver);
        return asCustomer;
    }

    public Trip driverAccept(UUID tripId, UUID driverId) {
        Trip t = get(tripId);
        User driver = users.findById(driverId).orElseThrow();
        t.setDriver(driver);
        t.setStatus(Enums.TripStatus.accepted);
        t.setAcceptedAt(Instant.now());
        return trips.save(t);
    }

    public Trip accept(UUID tripId) {
        Trip t = get(tripId);
        t.setStatus(Enums.TripStatus.accepted);
        t.setAcceptedAt(Instant.now());
        return trips.save(t);
    }

    public Trip verifyPin(UUID tripId, int pin) {
        Trip t = get(tripId);
        if (t.getPin() != pin) throw new RuntimeException("PIN inválido");
        t.setStatus(Enums.TripStatus.in_progress);
        return trips.save(t);
    }

    public Trip complete(UUID tripId) {
        Trip t = get(tripId);
        t.setStatus(Enums.TripStatus.completed);
        t.setCompletedAt(Instant.now());
        return trips.save(t);
    }

    public Trip cancelByCustomer(UUID tripId) {
        Trip t = get(tripId);
        t.setStatus(Enums.TripStatus.cancelled_by_customer);
        t.setCancelledAt(Instant.now());
        return trips.save(t);
    }

    public Trip cancelByDriver(UUID tripId) {
        Trip t = get(tripId);
        t.setStatus(Enums.TripStatus.cancelled_by_driver);
        t.setCancelledAt(Instant.now());
        return trips.save(t);
    }
}