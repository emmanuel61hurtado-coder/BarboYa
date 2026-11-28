package com.barboya.auth;

import com.barboya.audit.AuditEventRepository;
import com.barboya.auth.AuthDtos.AccessEvent;
import com.barboya.auth.AuthDtos.CodeRequest;
import com.barboya.auth.AuthDtos.ForgotPasswordRequest;
import com.barboya.auth.AuthDtos.LoginRequest;
import com.barboya.auth.AuthDtos.RegisterRequest;
import com.barboya.auth.AuthDtos.ResetPasswordRequest;
import com.barboya.auth.AuthDtos.SessionResponse;
import com.barboya.auth.AuthDtos.TokenResponse;
import com.barboya.common.ClientInfo;
import com.barboya.config.AppProperties;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_COOKIE = "barboya_refresh";

    private final AuthService auth;
    private final VerificationService verification;
    private final AuthSessionRepository sessions;
    private final AuditEventRepository auditEvents;
    private final boolean secureCookie;
    private final Clock clock;

    public AuthController(AuthService auth, VerificationService verification, AuthSessionRepository sessions,
                          AuditEventRepository auditEvents, AppProperties properties, Clock clock) {
        this.auth = auth;
        this.verification = verification;
        this.sessions = sessions;
        this.auditEvents = auditEvents;
        this.secureCookie = properties.security().cookieSecure();
        this.clock = clock;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest body,
                                                  HttpServletRequest request) {
        return withSession(HttpStatus.CREATED, auth.register(body, ClientInfo.from(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return withSession(HttpStatus.OK, auth.login(body, ClientInfo.from(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String token, HttpServletRequest request) {
        return withSession(HttpStatus.OK, auth.refresh(token, ClientInfo.from(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String token, HttpServletRequest request) {
        auth.logout(token, ClientInfo.from(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@AuthenticationPrincipal AuthUser user) {
        return sessions.findActive(user.id(), clock.instant()).stream()
                .map(s -> SessionResponse.from(s, user.sessionId()))
                .toList();
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id, HttpServletRequest request) {
        auth.revokeSession(user.id(), id, ClientInfo.from(request));
    }

    /** Cierra todas las sesiones menos la actual. */
    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeOthers(@AuthenticationPrincipal AuthUser user, HttpServletRequest request) {
        auth.revokeOtherSessions(user.id(), user.sessionId(), ClientInfo.from(request));
    }

    @GetMapping("/history")
    public List<AccessEvent> history(@AuthenticationPrincipal AuthUser user) {
        return auditEvents.findTop50ByTargetIdOrderByCreatedAtDesc(user.id()).stream()
                .map(AccessEvent::from)
                .toList();
    }

    @PostMapping("/email/verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestEmailVerification(@AuthenticationPrincipal AuthUser user) {
        verification.requestEmailVerification(user.id());
    }

    @PostMapping("/email/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CodeRequest body,
                            HttpServletRequest request) {
        verification.verifyEmail(user.id(), body.code(), ClientInfo.from(request));
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        verification.forgotPassword(body.email());
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest body, HttpServletRequest request) {
        verification.resetPassword(body, ClientInfo.from(request));
    }

    private ResponseEntity<TokenResponse> withSession(HttpStatus status, AuthService.Tokens tokens) {
        Duration remaining = Duration.between(clock.instant(), tokens.sessionExpiresAt());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken(), remaining).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new TokenResponse(tokens.accessToken(), "Bearer", tokens.accessSeconds(), tokens.user()));
    }

    /** HttpOnly: el JavaScript de la pagina no puede leer el token de refresco. */
    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
