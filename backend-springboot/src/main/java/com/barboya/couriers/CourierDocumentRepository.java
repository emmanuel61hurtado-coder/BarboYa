package com.barboya.couriers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierDocumentRepository extends JpaRepository<CourierDocument, UUID> {

    List<CourierDocument> findByUserId(UUID userId);

    Optional<CourierDocument> findByUserIdAndType(UUID userId, DocumentType type);
}
