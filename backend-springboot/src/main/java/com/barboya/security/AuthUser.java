package com.barboya.security;

import com.barboya.users.Role;

import java.util.UUID;

/** Identidad autenticada de la peticion, leida de la base y no del token. */
public record AuthUser(UUID id, UUID sessionId, Role role) {
}
