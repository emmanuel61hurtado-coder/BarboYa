package com.barboya.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private Instant emailVerifiedAt;
    private int failedLogins;
    private Instant lockedUntil;
    /** Cuando se elimino la cuenta; sus datos personales ya fueron reemplazados. */
    private Instant deletedAt;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public static User create(String fullName, String email, String phone, String passwordHash, Role role,
                              Instant now) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.fullName = fullName;
        user.email = email;
        user.phone = phone;
        user.passwordHash = passwordHash;
        user.role = role;
        user.status = role.initialStatus();
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isSuspended() {
        return status == UserStatus.SUSPENDED;
    }
}
