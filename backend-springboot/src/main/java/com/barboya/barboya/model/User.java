package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    private String fullName;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Enums.UserRole role = Enums.UserRole.customer;

    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // driver-related billing cached on user? keep minimal; DriverProfile holds it
}