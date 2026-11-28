package com.barboya.barboya.service;

import com.barboya.barboya.dto.DriversDtos;
import com.barboya.barboya.model.DriverProfile;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.DriverProfileRepository;
import com.barboya.barboya.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DriversService {
    private final DriverProfileRepository profiles;
    private final UserRepository users;

    public DriversService(DriverProfileRepository profiles, UserRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    public DriverProfile getOrCreate(UUID userId) {
        return profiles.findByUserId(userId).orElseGet(() -> {
            User u = users.findById(userId).orElseThrow();
            DriverProfile p = new DriverProfile();
            p.setUser(u);
            return profiles.save(p);
        });
    }

    public DriverProfile setLocation(UUID userId, DriversDtos.LocationUpdate req) {
        DriverProfile p = getOrCreate(userId);
        p.setLastLat(req.lat());
        p.setLastLng(req.lng());
        p.setLastSeenAt(java.time.Instant.now());
        return profiles.save(p);
    }

    public DriverProfile setOnline(UUID userId, boolean online) {
        DriverProfile p = getOrCreate(userId);
        p.setOnline(online);
        return profiles.save(p);
    }
}