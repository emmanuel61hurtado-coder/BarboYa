package com.barboya.barboya.service;

import com.barboya.barboya.dto.SecurityDtos;
import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.SecurityLog;
import com.barboya.barboya.model.Trip;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.SecurityLogRepository;
import com.barboya.barboya.repository.TripRepository;
import com.barboya.barboya.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SecurityService {
    private final SecurityLogRepository logs;
    private final TripRepository trips;
    private final UserRepository users;

    public SecurityService(SecurityLogRepository logs, TripRepository trips, UserRepository users) {
        this.logs = logs;
        this.trips = trips;
        this.users = users;
    }

    public SecurityLog log(UUID tripId, UUID userId, Enums.SecurityEventType type, String details, double lat, double lng) {
        SecurityLog l = new SecurityLog();
        if (tripId != null) l.setTrip(trips.findById(tripId).orElse(null));
        if (userId != null) l.setUser(users.findById(userId).orElse(null));
        l.setEventType(type);
        l.setDetails(details);
        l.setLat(lat);
        l.setLng(lng);
        return logs.save(l);
    }

    public String shareLink(UUID tripId) {
        Trip t = trips.findById(tripId).orElseThrow();
        if (t.getShareToken() == null) t.setShareToken(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        trips.save(t);
        return t.getShareToken();
    }
}