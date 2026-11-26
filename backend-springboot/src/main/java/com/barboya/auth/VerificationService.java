package com.barboya.auth;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.auth.AuthDtos.ResetPasswordRequest;
import com.barboya.auth.VerificationCode.Purpose;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.common.Contacts;
import com.barboya.config.AppProperties;
import com.barboya.notifications.MailRequested;
import com.barboya.notifications.MailService;
import com.barboya.security.SecretKeys;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Codigos de un solo uso enviados por correo: vencen, admiten pocos intentos y
 * se limita cuantos se piden. La verificacion por SMS requiere un proveedor.
 */
@Service
public class VerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final VerificationCodeRepository codes;
    private final AuthSessionRepository sessions;
    private final SecretKeys keys;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final AppProperties.Codes settings;
    private final Clock clock;

    public VerificationService(UserRepository users, VerificationCodeRepository codes, AuthSessionRepository sessions,
                               SecretKeys keys, PasswordEncoder encoder, MailService mail, AuditService audit,
                               ApplicationEventPublisher events, AppProperties properties, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.sessions = sessions;
        this.keys = keys;
        this.encoder = encoder;
        this.mail = mail;
        this.audit = audit;
        this.events = events;
        this.settings = properties.codes();
        this.clock = clock;
    }

    @Transactional
    public void requestEmailVerification(UUID userId) {
        mail.requireConfigured();
        User user = users.findById(userId).orElseThrow(VerificationService::invalidCode);
        if (user.getEmailVerifiedAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "Tu correo ya esta verificado");
        }
        if (!issue(user, Purpose.EMAIL_VERIFICATION, "Verifica tu correo en BarboYa")) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CODE_LIMIT",
                    "Ya enviamos un codigo; espera antes de pedir otro");
        }
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void verifyEmail(UUID userId, String code, ClientInfo client) {
        User user = users.findById(userId).orElseThrow(VerificationService::invalidCode);
        consume(user.getId(), Purpose.EMAIL_VERIFICATION, code);
        user.setEmailVerifiedAt(clock.instant());
        user.setUpdatedAt(clock.instant());
        audit.record(AuditAction.EMAIL_VERIFIED, userId, userId, null, client);
    }

    /** Responde igual exista o no la cuenta, para no revelar quien esta registrado. */
    @Transactional
    public void forgotPassword(String email) {
        mail.requireConfigured();
        Optional<User> user = users.lockByEmail(Contacts.email(email));
        user.filter(u -> !u.isSuspended())
                .ifPresent(u -> issue(u, Purpose.PASSWORD_RESET, "Restablece tu contrasena de BarboYa"));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void resetPassword(ResetPasswordRequest request, ClientInfo client) {
        User user = users.lockByEmail(Contacts.email(request.email()))
                .filter(u -> !u.isSuspended())
                .orElseThrow(VerificationService::invalidCode);
        consume(user.getId(), Purpose.PASSWORD_RESET, request.code());
        Instant now = clock.instant();
        user.setPasswordHash(encoder.encode(request.newPassword()));
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        // Recibir el codigo demuestra que el correo es suyo
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(now);
        }
        user.setUpdatedAt(now);
        audit.record(AuditAction.PASSWORD_RESET, user.getId(), user.getId(), null, client);
        sessions.revokeAll(user.getId(), now);
    }

    /** @return false si se alcanzo el limite de envios */
    private boolean issue(User user, Purpose purpose, String subject) {
        Instant now = clock.instant();
        Optional<VerificationCode> last =
                codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose);
        boolean tooSoon = last.isPresent()
                && last.get().getCreatedAt().plus(settings.resendCooldown()).isAfter(now);
        long today = codes.countByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose,
                now.minus(Duration.ofDays(1)));
        if (tooSoon || today >= settings.maxPerDay()) {
            return false;
        }
        // Solo vale el codigo mas reciente: la consulta de canje toma el ultimo
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        codes.save(new VerificationCode(user.getId(), purpose, fingerprint(user.getId(), purpose, code),
                now, now.plus(settings.ttl())));
        events.publishEvent(new MailRequested(user.getEmail(), subject,
                "Tu codigo es " + code + ". Vence en " + settings.ttl().toMinutes()
                        + " minutos. Si no lo pediste, ignora este mensaje."));
        return true;
    }

    private void consume(UUID userId, Purpose purpose, String code) {
        VerificationCode stored = codes.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .filter(c -> c.isUsable(clock.instant(), settings.maxAttempts()))
                .orElseThrow(VerificationService::invalidCode);
        boolean matches = MessageDigest.isEqual(
                stored.getCodeHash().getBytes(StandardCharsets.US_ASCII),
                fingerprint(userId, purpose, code).getBytes(StandardCharsets.US_ASCII));
        if (!matches) {
            stored.registerFailure();
            throw invalidCode();
        }
        stored.consume(clock.instant());
    }

    private String fingerprint(UUID userId, Purpose purpose, String code) {
        return keys.codeFingerprint(userId + ":" + purpose + ":" + code);
    }

    private static ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CODE", "El codigo no es valido o ya vencio");
    }
}
