package com.barboya.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Bloqueado: un mismo token no puede canjearse dos veces a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> lockByHash(@Param("hash") String hash);

    /** El token vigente que nacio de otro, si lo hay. */
    Optional<RefreshToken> findByParentIdAndDiscardedAtIsNull(UUID parentId);
}
