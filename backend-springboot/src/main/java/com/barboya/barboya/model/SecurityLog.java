package com.barboya.barboya.model;

import com.barboya.barboya.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_logs")
@Getter
@Setter
public class SecurityLog {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Enums.SecurityEventType eventType;

    @Column(columnDefinition = "text")
    private String details;

    private double lat;
    private double lng;
    private Instant createdAt = Instant.now();
}