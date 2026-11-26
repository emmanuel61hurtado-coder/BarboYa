package com.barboya.barboya.repository;

import com.barboya.barboya.model.DriverProfile;
import com.barboya.barboya.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    Optional<DriverProfile> findByUser(User user);
    Optional<DriverProfile> findByUserId(UUID userId);
}