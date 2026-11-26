package com.barboya.couriers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourierPresenceRepository extends JpaRepository<CourierPresence, UUID> {

    List<CourierPresence> findByOnlineTrue();
}
