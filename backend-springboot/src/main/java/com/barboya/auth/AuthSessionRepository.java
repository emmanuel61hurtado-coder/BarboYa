package com.barboya.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @Query("""
            select s from AuthSession s
            where s.userId = :userId and s.revokedAt is null and s.expiresAt > :now
            order by s.lastUsedAt desc""")
    List<AuthSession> findActive(@Param("userId") UUID userId, @Param("now") Instant now);

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndUserAgentAndIp(UUID userId, String userAgent, String ip);

    @Modifying(flushAutomatically = true)
    @Query("update AuthSession s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
    int revokeAll(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query("""
            update AuthSession s set s.revokedAt = :now
            where s.userId = :userId and s.revokedAt is null and s.id <> :keep""")
    int revokeOthers(@Param("userId") UUID userId, @Param("keep") UUID keep, @Param("now") Instant now);
}
