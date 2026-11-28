package com.barboya.couriers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourierProfileRepository extends JpaRepository<CourierProfile, UUID> {

    boolean existsByDocumentNumberAndUserIdNot(String documentNumber, UUID userId);
}
