package com.barboya.admin;

import com.barboya.audit.AuditEvent;
import com.barboya.auth.AuthDtos;
import com.barboya.users.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record ChangeStatusRequest(
            @NotNull UserStatus status,
            @NotBlank @Size(min = 5, max = 300) String reason) {
    }

    public record CreateAdminRequest(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{10,20}$", message = "Celular no valido") String phone,
            @NotBlank @Size(min = 12, max = 128)
            @Pattern(regexp = AuthDtos.PASSWORD_PATTERN, message = AuthDtos.PASSWORD_MESSAGE) String password) {

        @Override
        public String toString() {
            return "CreateAdminRequest[]";
        }
    }

    public record AuditResponse(
            UUID id, UUID actorId, UUID targetId, String action, String detail, String ip, Instant at) {

        static AuditResponse from(AuditEvent e) {
            return new AuditResponse(e.getId(), e.getActorId(), e.getTargetId(), e.getAction().name(),
                    e.getDetail(), e.getIp(), e.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long total) {
    }
}
