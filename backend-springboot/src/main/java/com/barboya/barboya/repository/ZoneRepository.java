package com.barboya.barboya.repository;

import com.barboya.barboya.model.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
}