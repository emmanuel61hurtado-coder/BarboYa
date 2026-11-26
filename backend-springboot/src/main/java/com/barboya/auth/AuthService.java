package com.barboya.auth;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.auth.AuthDtos.LoginRequest;
import com.barboya.auth.AuthDtos.RegisterRequest;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.common.Contacts;
import com.barboya.config.AppProperties;
import com.barboya.notifications.MailRequested;
import com.barboya.security.JwtService;
import com.barboya.security.SecretKeys;
import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import com.barboya.users.UserResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Los metodos que registran intentos fallidos usan {@code noRollbackFor}: el
 * contador, el bloqueo y la auditoria deben quedar guardados aunque se responda
 * con error.
 */
@Service
public class AuthService {

    /** Resultado de abrir o renovar una sesion. */
    public record Tokens(String accessToken, long accessSeconds, String refreshToken, Instant sessionExpiresAt,
                         UserResponse user) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final AppProperties.Security settings;
    private final Clock clock;
    /** Iguala el tiempo de respuesta cuando la cuenta no existe. */
    private final String decoyHash;

    public AuthService(UserRepository users, AuthSessionRepository sessions, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt, AuditService audit,
                       ApplicationEventPublisher events, AppProperties properties, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.events = events;
        this.settings = properties.security();
        this.clock = clock;
        this.decoyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Tokens register(RegisterRequest request, ClientInfo client) {
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (role.isStaff()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ROLE_NOT_ALLOWED",
                    "Ese rol no se puede registrar de forma publica");
        }
        String email = Contacts.email(request.email());
        String phone = Contacts.phone(request.phone());
        if (!phone.matches("^\\+\\d{10,15}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "Celular no valido");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Ese correo ya esta registrado");
        }
        if (users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_TAKEN", "Ese celular ya esta registrado");
        }
        User user = users.saveAndFlush(User.create(request.fullName().trim(), email, phone,
                encoder.encode(request.password()), role, clock.instant()));
        audit.record(AuditAction.REGISTERED, user.getId(), user.getId(), role.name(), client);
        return openSession(user, client);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Tokens login(LoginRequest request, ClientInfo client) {
        String identifier = request.identifier().trim();
        Optional<User> found = identifier.contains("@")
                ? users.lockByEmail(Contacts.email(identifier))
                : users.lockByPhone(Contacts.phone(identifier));
        if (found.isEmpty()) {
            encoder.matches(request.password(), decoyHash);
            audit.record(AuditAction.LOGIN_FAILED, null, null, "cuenta inexistente", client);
            throw invalidCredentials();
        }

        User user = found.get();
        Instant now = clock.instant();
        boolean passwordMatches = encoder.matches(request.password(), user.getPasswordHash());
        if (user.isLocked(now)) {
            audit.record(AuditAction.LOGIN_BLOCKED, null, user.getId(), null, client);
            throw invalidCredentials();
        }
        if (!passwordMatches) {
            user.setFailedLogins(user.getFailedLogins() + 1);
            if (user.getFailedLogins() >= settings.maxFailedLogins()) {
                user.setFailedLogins(0);
                user.setLockedUntil(now.plus(settings.lockDuration()));
                audit.record(AuditAction.LOGIN_LOCKED, null, user.getId(), null, client);
            } else {
                audit.record(AuditAction.LOGIN_FAILED, null, user.getId(), null, client);
            }
            throw invalidCredentials();
        }
        if (user.isSuspended()) {
            audit.record(AuditAction.LOGIN_BLOCKED, user.getId(), user.getId(), "cuenta suspendida", client);
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "Tu cuenta esta suspendida");
        }

        user.setFailedLogins(0);
        user.setLockedUntil(null);
        boolean newDevice = sessions.existsByUserId(user.getId())
                && !sessions.existsByUserIdAndUserAgentAndIp(user.getId(), client.userAgent(), client.ip());
        if (newDevice) {
            audit.record(AuditAction.LOGIN_NEW_DEVICE, user.getId(), user.getId(), null, client);
            events.publishEvent(new MailRequested(user.getEmail(), "Nuevo inicio de sesion en BarboYa",
                    "Se inicio sesion en tu cuenta desde un dispositivo o red que no habiamos visto. "
                            + "Si no fuiste tu, cierra las sesiones activas y cambia tu contrasena."));
        }
        audit.record(AuditAction.LOGIN_SUCCESS, user.getId(), user.getId(), null, client);
        return openSession(user, client);
    }

    /**
     * Cada token de refresco sirve una vez; reutilizarlo revela una copia y cierra
     * la sesion. Unica excepcion: si la respuesta se perdio (recarga, corte de red),
     * el mismo token puede repetirse durante unos segundos mientras su reemplazo
     * siga sin usarse.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Tokens refresh(String rawToken, ClientInfo client) {
        RefreshToken token = findToken(rawToken).orElseThrow(AuthService::invalidSession);
        AuthSession session = sessions.findById(token.getSessionId()).orElseThrow(AuthService::invalidSession);
        Instant now = clock.instant();
        Optional<RefreshToken> unclaimed = unclaimedReplacement(token, now);
        if (token.getDiscardedAt() != null || (token.getUsedAt() != null && unclaimed.isEmpty())) {
            session.revoke(now);
            audit.record(AuditAction.REFRESH_TOKEN_REUSED, null, session.getUserId(),
                    session.getId().toString(), client);
            throw invalidSession();
        }
        User user = users.findById(session.getUserId()).orElseThrow(AuthService::invalidSession);
        if (!session.isActive(now) || user.isSuspended()) {
            throw invalidSession();
        }
        if (token.getUsedAt() == null) {
            token.markUsed(now);
        } else {
            unclaimed.ifPresent(replacement -> replacement.discard(now));
        }
        session.touch(now);
        return issue(user, session, token.getId());
    }

    /** Reemplazo que nadie ha usado, solo si el original se gasto hace muy poco. */
    private Optional<RefreshToken> unclaimedReplacement(RefreshToken token, Instant now) {
        if (token.getUsedAt() == null || token.getUsedAt().plus(settings.refreshRetryWindow()).isBefore(now)) {
            return Optional.empty();
        }
        return refreshTokens.findByParentIdAndDiscardedAtIsNull(token.getId())
                .filter(replacement -> replacement.getUsedAt() == null);
    }

    @Transactional
    public void logout(String rawToken, ClientInfo client) {
        findToken(rawToken)
                .flatMap(token -> sessions.findById(token.getSessionId()))
                .ifPresent(session -> {
                    session.revoke(clock.instant());
                    audit.record(AuditAction.LOGOUT, session.getUserId(), session.getUserId(),
                            session.getId().toString(), client);
                });
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId, ClientInfo client) {
        AuthSession session = sessions.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND",
                        "Sesion no encontrada"));
        session.revoke(clock.instant());
        audit.record(AuditAction.SESSION_REVOKED, userId, userId, sessionId.toString(), client);
    }

    @Transactional
    public void revokeOtherSessions(UUID userId, UUID currentSessionId, ClientInfo client) {
        int revoked = sessions.revokeOthers(userId, currentSessionId, clock.instant());
        audit.record(AuditAction.SESSION_REVOKED, userId, userId, "otras sesiones: " + revoked, client);
    }

    private Optional<RefreshToken> findToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokens.lockByHash(SecretKeys.sha256(rawToken));
    }

    private Tokens openSession(User user, ClientInfo client) {
        Instant now = clock.instant();
        AuthSession session = sessions.save(
                new AuthSession(user.getId(), client, now, now.plus(settings.sessionTtl())));
        return issue(user, session, null);
    }

    private Tokens issue(User user, AuthSession session, UUID parentTokenId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(
                session.getId(), parentTokenId, SecretKeys.sha256(raw), clock.instant()));
        return new Tokens(jwt.create(user.getId(), session.getId()), settings.accessTokenTtl().toSeconds(),
                raw, session.getExpiresAt(), UserResponse.from(user));
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Datos incorrectos. Tras varios intentos fallidos la cuenta se bloquea unos minutos");
    }

    private static ApiException invalidSession() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SESSION", "Tu sesion termino; ingresa de nuevo");
    }
}
