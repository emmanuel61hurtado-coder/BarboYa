package com.barboya.barboya.service;

import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.DriverProfile;
import com.barboya.barboya.model.Zone;
import com.barboya.barboya.repository.DriverProfileRepository;
import com.barboya.barboya.repository.ZoneRepository;
import org.springframework.stereotype.Service;

@Service
public class BillingService {
    private final DriverProfileRepository profiles;
    private final ZoneRepository zones;
    private final com.barboya.barboya.config.BarboyaProperties props;

    public BillingService(DriverProfileRepository profiles, ZoneRepository zones, com.barboya.barboya.config.BarboyaProperties props) {
        this.profiles = profiles;
        this.zones = zones;
        this.props = props;
    }

    public boolean canAccept(DriverProfile p) {
        if (p.getBillingScheme() == Enums.BillingScheme.subscription) {
            return p.getSubscriptionExpiresAtEpoch() > System.currentTimeMillis();
        }
        // per_trip_fee
        int fee = resolveFee(p, null);
        return p.getWalletBalanceCop() >= fee;
    }

    public int resolveFee(DriverProfile p, Zone zone) {
        if (p.getTripFeeOverrideCop() > 0) return p.getTripFeeOverrideCop();
        if (zone != null) return zone.getPerTripFeeCop();
        return props.getDefaultTripFeeCop();
    }

    public void chargeOnComplete(DriverProfile p, Zone zone) {
        if (p.getBillingScheme() == Enums.BillingScheme.per_trip_fee) {
            int fee = resolveFee(p, zone);
            p.setWalletBalanceCop(p.getWalletBalanceCop() - fee);
            profiles.save(p);
        }
    }
}