package com.barboya.auth;

import com.barboya.audit.AuditEvent;
import com.barboya.users.Role;
import com.barboya.users.UserResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    /** 10 a 128 caracteres, con al menos una letra y un numero. */
    public static final String PASSWORD_PATTERN = "^(?=.*\\p{L})(?=.*\\d).{10,128}$";
    public static final String PASSWORD_MESSAGE =
            "Usa entre 10 y 128 caracteres, con al menos una letra y un numero";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{10,20}$", message = "Celular no valido") String phone,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String password,
            Role role,
            Boolean adult,
            @Size(max = 40) String termsVersion,
            @Size(max = 40) String privacyVersion) {

        @Override
        public String toString() {
            return "RegisterRequest[role=" + role + "]";
        }
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String identifier,
            @NotBlank @Size(max = 128) String password) {

        @Override
        public String toString() {
            return "LoginRequest[]";
        }
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
    }

    public record CodeRequest(@NotBlank @Pattern(regexp = "^\\d{6}$", message = "El codigo tiene 6 digitos") String code) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "El codigo tiene 6 digitos") String code,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String newPassword) {

        @Override
        public String toString() {
            return "ResetPasswordRequest[]";
        }
    }

    public record SessionResponse(
            UUID id, boolean current, String ip, String userAgent, Instant createdAt, Instant lastUsedAt) {

        static SessionResponse from(AuthSession session, UUID currentId) {
            return new SessionResponse(session.getId(), session.getId().equals(currentId), session.getIp(),
                    session.getUserAgent(), session.getCreatedAt(), session.getLastUsedAt());
        }
    }

    public record AccessEvent(String action, String ip, String userAgent, Instant at) {

        static AccessEvent from(AuditEvent event) {
            return new AccessEvent(event.getAction().name(), event.getIp(), event.getUserAgent(), event.getCreatedAt());
        }
    }
}
