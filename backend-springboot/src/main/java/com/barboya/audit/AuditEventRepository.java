package com.barboya.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findTop50ByTargetIdOrderByCreatedAtDesc(UUID targetId);

    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
