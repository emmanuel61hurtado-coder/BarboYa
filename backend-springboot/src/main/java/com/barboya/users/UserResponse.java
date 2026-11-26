package com.barboya.users;

import java.time.Instant;
import java.util.UUID;

/** Vista publica de una cuenta. Nunca incluye el hash ni los contadores de bloqueo. */
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getEmailVerifiedAt() != null, user.getCreatedAt());
    }
}
