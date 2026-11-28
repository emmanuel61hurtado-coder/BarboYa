package com.barboya.barboya.repository;

import com.barboya.barboya.model.SecurityLog;
import com.barboya.barboya.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecurityLogRepository extends JpaRepository<SecurityLog, UUID> {
    List<SecurityLog> findByTrip(Trip trip);
}