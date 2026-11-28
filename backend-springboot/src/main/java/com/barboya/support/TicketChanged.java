package com.barboya.support;

import java.util.UUID;

/** El equipo respondio o cerro un caso: avisar a quien lo abrio. */
public record TicketChanged(UUID ticketId, UUID openerId) {
}
