package com.barboya.admin;

import com.barboya.admin.AdminDtos.ChangeStatusRequest;
import com.barboya.admin.AdminDtos.CreateAdminRequest;
import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.auth.AuthSessionRepository;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.common.Contacts;
import com.barboya.security.AuthUser;
import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import com.barboya.users.UserResponse;
import com.barboya.users.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final Clock clock;

    public AdminService(UserRepository users, AuthSessionRepository sessions, PasswordEncoder encoder,
                        AuditService audit, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
    }

    /** Aprobar (tras revision humana), suspender o reactivar una cuenta. */
    @Transactional
    public UserResponse changeStatus(AuthUser actor, UUID targetId, ChangeStatusRequest request, ClientInfo client) {
        if (request.status() == UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Elige activar o suspender");
        }
        if (actor.id().equals(targetId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SELF_CHANGE", "No puedes cambiar tu propio estado");
        }
        User target = users.findById(targetId).orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado"));
        if (target.getRole().isStaff() && actor.role() != Role.SUPERADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Solo un superadministrador gestiona cuentas internas");
        }
        UserStatus previous = target.getStatus();
        Instant now = clock.instant();
        target.setStatus(request.status());
        target.setUpdatedAt(now);
        audit.record(AuditAction.USER_STATUS_CHANGED, actor.id(), targetId,
                previous + " -> " + request.status() + ": " + request.reason().trim(), client);
        UserResponse response = UserResponse.from(users.saveAndFlush(target));
        if (request.status() == UserStatus.SUSPENDED) {
            sessions.revokeAll(targetId, now);
        }
        return response;
    }

    @Transactional
    public UserResponse createAdmin(AuthUser actor, CreateAdminRequest request, ClientInfo client) {
        String email = Contacts.email(request.email());
        String phone = Contacts.phone(request.phone());
        if (users.existsByEmail(email) || users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONTACT_TAKEN", "El correo o celular ya esta registrado");
        }
        User admin = users.saveAndFlush(User.create(request.fullName().trim(), email, phone,
                encoder.encode(request.password()), Role.ADMIN, clock.instant()));
        audit.record(AuditAction.STAFF_CREATED, actor.id(), admin.getId(), Role.ADMIN.name(), client);
        return UserResponse.from(admin);
    }
}
