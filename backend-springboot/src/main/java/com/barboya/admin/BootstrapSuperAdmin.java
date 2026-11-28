package com.barboya.admin;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.auth.AuthDtos;
import com.barboya.common.ClientInfo;
import com.barboya.common.Contacts;
import com.barboya.config.AppProperties;
import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Crea el primer superadministrador a partir de variables de entorno, una sola vez. */
@Component
public class BootstrapSuperAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSuperAdmin.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final AppProperties.BootstrapAdmin settings;
    private final Clock clock;

    public BootstrapSuperAdmin(UserRepository users, PasswordEncoder encoder, AuditService audit,
                               AppProperties properties, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
        this.settings = properties.bootstrapAdmin();
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (settings.email().isBlank() || users.existsByRole(Role.SUPERADMIN)) {
            return;
        }
        if (settings.phone().isBlank() || settings.password().length() < 12
                || !settings.password().matches(AuthDtos.PASSWORD_PATTERN)) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PHONE es obligatorio y BOOTSTRAP_ADMIN_PASSWORD "
                    + "necesita al menos 12 caracteres con letras y numeros");
        }
        User admin = users.save(User.create("Superadministrador", Contacts.email(settings.email()),
                Contacts.phone(settings.phone()), encoder.encode(settings.password()), Role.SUPERADMIN,
                clock.instant()));
        audit.record(AuditAction.STAFF_CREATED, null, admin.getId(), "arranque inicial", ClientInfo.system());
        log.info("Superadministrador inicial creado. Elimina BOOTSTRAP_ADMIN_PASSWORD del entorno.");
    }
}
