package com.barboya.legal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, UUID> {

    List<LegalAcceptance> findByUserId(UUID userId);
}
