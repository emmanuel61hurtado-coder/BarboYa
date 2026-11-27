package com.barboya.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByOpenerId(UUID openerId, Pageable pageable);

    Page<Ticket> findByStatusIn(Collection<Ticket.Status> statuses, Pageable pageable);

    long countByOpenerIdAndStatusNot(UUID openerId, Ticket.Status status);

    boolean existsByOpenerIdAndOrderIdAndStatusNot(UUID openerId, UUID orderId, Ticket.Status status);
}
