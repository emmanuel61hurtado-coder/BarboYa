package com.barboya.barboya.config;

import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.User;
import com.barboya.barboya.model.Zone;
import com.barboya.barboya.repository.UserRepository;
import com.barboya.barboya.repository.ZoneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final ZoneRepository zones;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository users, ZoneRepository zones, PasswordEncoder encoder) {
        this.users = users;
        this.zones = zones;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // seed demo customer
        if (users.findByPhone("+573001112233").isEmpty()) {
            User u = new User();
            u.setPhone("+573001112233");
            u.setFullName("Pasajero Demo");
            u.setPasswordHash(encoder.encode("Cliente123!"));
            u.setRole(Enums.UserRole.customer);
            users.save(u);
        }
        // seed zones if empty
        if (zones.count() == 0) {
            Zone z1 = new Zone();
            z1.setName("Barbosa Casco Urbano");
            z1.setUrban(true);
            z1.setBoundary("POLYGON((-73.630 5.925, -73.610 5.925, -73.610 5.942, -73.630 5.942, -73.630 5.925))");
            z1.setBaseFareCop(3000);
            z1.setPerKmCop(800);
            z1.setNightSurchargePct(20);
            z1.setBillingScheme("per_trip_fee");
            z1.setPerTripFeeCop(300);
            zones.save(z1);

            Zone z2 = new Zone();
            z2.setName("Barbosa Rural");
            z2.setUrban(false);
            z2.setBoundary("POLYGON((-73.680 5.880, -73.560 5.880, -73.560 6.000, -73.680 6.000, -73.680 5.880))");
            z2.setBaseFareCop(5000);
            z2.setPerKmCop(1200);
            z2.setNightSurchargePct(25);
            z2.setBillingScheme("per_trip_fee");
            z2.setPerTripFeeCop(500);
            zones.save(z2);
        }
    }
}