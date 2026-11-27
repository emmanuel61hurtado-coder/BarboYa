package com.barboya.support;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "support_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketMessage {

    @Id
    private UUID id;
    private UUID ticketId;
    private UUID authorId;
    private boolean staff;
    private String body;
    private Instant createdAt;

    TicketMessage(UUID ticketId, UUID authorId, boolean staff, String body, Instant now) {
        this.id = UUID.randomUUID();
        this.ticketId = ticketId;
        this.authorId = authorId;
        this.staff = staff;
        this.body = body;
        this.createdAt = now;
    }
}
