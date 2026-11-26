package com.barboya.barboya.repository;

import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.Trip;
import com.barboya.barboya.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findByCustomer(User customer);
    List<Trip> findByDriver(User driver);
    List<Trip> findByStatus(Enums.TripStatus status);
}