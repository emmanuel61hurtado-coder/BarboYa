package com.barboya.barboya.service;

import com.barboya.barboya.dto.AdminDtos;
import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.DriverProfile;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.DriverProfileRepository;
import com.barboya.barboya.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final UserRepository users;
    private final DriverProfileRepository profiles;

    public AdminService(UserRepository users, DriverProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    public void updateBilling(AdminDtos.BillingUpdateRequest req) {
        DriverProfile p = profiles.findByUserId(req.driverId()).orElseThrow();
        p.setBillingScheme(Enums.BillingScheme.valueOf(req.scheme()));
        p.setTripFeeOverrideCop(req.tripFeeOverrideCop());
        profiles.save(p);
    }

    public void activateSubscription(UUID driverId, String plan) {
        DriverProfile p = profiles.findByUserId(driverId).orElseThrow();
        long now = System.currentTimeMillis();
        long expires = switch (plan) {
            case "daily" -> now + 86400000L;
            case "weekly" -> now + 7 * 86400000L;
            default -> now + 30 * 86400000L;
        };
        p.setSubscriptionExpiresAtEpoch(expires);
        p.setBillingScheme(Enums.BillingScheme.subscription);
        profiles.save(p);
    }

    public List<DriverProfile> drivers() {
        return profiles.findAll();
    }
}