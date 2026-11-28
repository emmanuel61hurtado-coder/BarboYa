package com.barboya.account;

import com.barboya.addresses.AddressService;
import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.auth.AuthSessionRepository;
import com.barboya.businesses.BusinessService;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.couriers.CourierService;
import com.barboya.finance.LedgerService;
import com.barboya.orders.OrderAccess;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import com.barboya.users.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Elimina una cuenta (Ley 1581 de 2012, derecho de supresion): reemplaza los
 * datos personales, borra direcciones y documentos y cierra las sesiones. Se
 * conserva lo que prueba operaciones realizadas: pedidos, comisiones, casos,
 * auditoria y las autorizaciones dadas.
 */
@Service
@Transactional
public class AccountDeletionService {

    private final UserRepository users;
    private final OrderAccess orders;
    private final LedgerService ledger;
    private final AddressService addresses;
    private final CourierService couriers;
    private final BusinessService businesses;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final Clock clock;

    public AccountDeletionService(UserRepository users, OrderAccess orders, LedgerService ledger,
                                  AddressService addresses, CourierService couriers, BusinessService businesses,
                                  AuthSessionRepository sessions, PasswordEncoder encoder, AuditService audit,
                                  Clock clock) {
        this.users = users;
        this.orders = orders;
        this.ledger = ledger;
        this.addresses = addresses;
        this.couriers = couriers;
        this.businesses = businesses;
        this.sessions = sessions;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
    }

    public void delete(UUID userId, ClientInfo client) {
        User user = users.lockById(userId).orElseThrow();
        if (user.getRole().isStaff()) {
            throw conflict("STAFF_ACCOUNT", "Las cuentas del equipo las retira un superadministrador");
        }
        if (orders.hasActiveOrders(userId)) {
            throw conflict("ACTIVE_ORDERS", "Tienes pedidos en curso. Termina o cancela esos pedidos primero");
        }
        if (ledger.balanceOf(userId) > 0) {
            throw conflict("PENDING_DEBT", "Tienes comisiones pendientes de pago. Ponte al dia primero");
        }
        Instant now = clock.instant();
        addresses.removeAll(userId);
        couriers.forget(userId);
        businesses.closeFor(userId);

        String token = userId.toString().replace("-", "");
        user.setFullName("Cuenta eliminada");
        user.setEmail("eliminada-" + token + "@eliminada.invalid");
        // No es un numero: nadie puede ingresar ni registrarse con el
        user.setPhone("x" + token.substring(0, 15));
        user.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.SUSPENDED);
        user.setEmailVerifiedAt(null);
        user.setDeletedAt(now);
        user.setUpdatedAt(now);
        audit.record(AuditAction.ACCOUNT_DELETED, userId, userId, user.getRole().name(), client);
        users.flush();
        sessions.revokeAll(userId, now);
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
