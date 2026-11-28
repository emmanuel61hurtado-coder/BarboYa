package com.barboya.finance;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.config.FinanceProperties;
import com.barboya.finance.FinanceDtos.AdjustmentRequest;
import com.barboya.finance.FinanceDtos.DebtorView;
import com.barboya.finance.FinanceDtos.Movement;
import com.barboya.finance.FinanceDtos.PaymentRequest;
import com.barboya.finance.FinanceDtos.Period;
import com.barboya.finance.FinanceDtos.Statement;
import com.barboya.finance.FinanceDtos.Summary;
import com.barboya.finance.LedgerEntry.Kind;
import com.barboya.finance.LedgerLine.Account;
import com.barboya.users.Role;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Libro de doble partida. Las cuentas registran lo que comercios y domiciliarios
 * deben a BarboYa; el dinero de los pedidos en efectivo no pasa por aqui.
 */
@Service
@Transactional
public class LedgerService {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final LedgerEntryRepository entries;
    private final LedgerLineRepository lines;
    private final UserRepository users;
    private final AuditService audit;
    private final FinanceProperties settings;
    private final Clock clock;

    public LedgerService(LedgerEntryRepository entries, LedgerLineRepository lines, UserRepository users,
                         AuditService audit, FinanceProperties settings, Clock clock) {
        this.entries = entries;
        this.lines = lines;
        this.users = users;
        this.audit = audit;
        this.settings = settings;
        this.clock = clock;
    }

    /** Se llama dentro de la transaccion de la entrega: ambas se guardan o ninguna. */
    public void chargeCommission(UUID partyId, UUID orderId, int amountCop) {
        if (amountCop > 0) {
            post(new LedgerEntry(Kind.COMMISSION, partyId, orderId, amountCop, null, null, null, null, null,
                    clock.instant()), Account.RECEIVABLE, Account.COMMISSION_REVENUE);
        }
    }

    /** Comisiones pendientes de pago. */
    @Transactional(readOnly = true)
    public long balanceOf(UUID partyId) {
        return lines.balanceOf(partyId);
    }

    @Transactional(readOnly = true)
    public boolean isOverLimit(UUID partyId) {
        return lines.balanceOf(partyId) > settings.debtLimitCop();
    }

    @Transactional(readOnly = true)
    public Set<UUID> overLimit(Collection<UUID> parties) {
        return parties.isEmpty() ? Set.of() : new HashSet<>(lines.over(parties, settings.debtLimitCop()));
    }

    /** Pago que BarboYa ya recibio por fuera, con su comprobante. */
    public Statement registerPayment(UUID actorId, UUID partyId, PaymentRequest request, ClientInfo client) {
        User party = lockParty(partyId);
        String reference = request.reference().trim();
        entries.lockReference(request.method() + ":" + reference.toLowerCase());
        if (entries.referenceInUse(request.method(), reference)) {
            throw new ApiException(HttpStatus.CONFLICT, "REFERENCE_IN_USE", "Esa referencia ya fue registrada");
        }
        requireWithinBalance(partyId, request.amountCop());
        post(new LedgerEntry(Kind.PAYMENT, partyId, null, request.amountCop(), request.method(), reference,
                blankToNull(request.note()), null, actorId, clock.instant()),
                Account.PLATFORM_FUNDS, Account.RECEIVABLE);
        audit.record(AuditAction.PAYMENT_REGISTERED, actorId, partyId,
                request.amountCop() + " COP " + request.method() + " " + reference, client);
        return statement(party, 0);
    }

    /** Perdona parte de la deuda, dejando el motivo. */
    public Statement waive(UUID actorId, UUID partyId, AdjustmentRequest request, ClientInfo client) {
        User party = lockParty(partyId);
        requireWithinBalance(partyId, request.amountCop());
        post(new LedgerEntry(Kind.ADJUSTMENT, partyId, null, request.amountCop(), null, null,
                request.reason().trim(), null, actorId, clock.instant()), Account.WAIVED, Account.RECEIVABLE);
        audit.record(AuditAction.DEBT_ADJUSTED, actorId, partyId,
                request.amountCop() + " COP: " + request.reason().trim(), client);
        return statement(party, 0);
    }

    /** Anula un pago o ajuste mal registrado con un asiento contrario. */
    public Statement reverse(UUID actorId, UUID entryId, String reason, ClientInfo client) {
        LedgerEntry original = entries.findById(entryId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "ENTRY_NOT_FOUND", "Movimiento no encontrado"));
        User party = lockParty(original.getPartyId());
        if (original.getKind() != Kind.PAYMENT && original.getKind() != Kind.ADJUSTMENT) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_REVERSIBLE",
                    "Solo se reversan pagos y ajustes");
        }
        if (entries.existsByReversalOf(entryId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REVERSED", "Ese movimiento ya fue reversado");
        }
        LedgerEntry reversal = entries.save(new LedgerEntry(Kind.REVERSAL, party.getId(), null,
                original.getAmountCop(), null, null, reason.trim(), entryId, actorId, clock.instant()));
        lines.saveAll(lines.findByEntryId(entryId).stream()
                .map(line -> new LedgerLine(reversal.getId(), line.getAccount(), line.getPartyId(),
                        line.getCreditCop(), line.getDebitCop()))
                .toList());
        entries.flush();
        audit.record(AuditAction.ENTRY_REVERSED, actorId, party.getId(),
                original.getKind() + " " + entryId + ": " + reason.trim(), client);
        return statement(party, 0);
    }

    @Transactional(readOnly = true)
    public Statement statementOf(UUID partyId, int page) {
        return statement(requireParty(users.findById(partyId).orElse(null)), page);
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        Instant today = LocalDate.ofInstant(clock.instant(), COLOMBIA).atStartOfDay(COLOMBIA).toInstant();
        return new Summary(
                period(today),
                period(today.minus(Duration.ofDays(6))),
                period(today.minus(Duration.ofDays(29))),
                lines.net(Account.RECEIVABLE),
                lines.net(Account.PLATFORM_FUNDS),
                lines.net(Account.WAIVED),
                lines.debtors().stream()
                        .map(d -> new DebtorView(d.getPartyId(), d.getName(), d.getRole(), d.getBalanceCop(),
                                d.getBalanceCop() > settings.debtLimitCop()))
                        .toList());
    }

    private Period period(Instant since) {
        var activity = lines.activitySince(since);
        return new Period(activity.getOrders(), activity.getSalesCop(), activity.getCommissionsCop());
    }

    private Statement statement(User party, int page) {
        var result = entries.findByPartyIdOrderByCreatedAtDesc(party.getId(),
                PageRequest.of(Math.max(page, 0), 20));
        Set<UUID> reversed = entries
                .findByReversalOfIn(result.getContent().stream().map(LedgerEntry::getId).toList()).stream()
                .map(LedgerEntry::getReversalOf)
                .collect(Collectors.toSet());
        List<Movement> movements = result.getContent().stream()
                .map(e -> new Movement(e.getId(), e.getKind(), effect(e), e.getOrderId(), e.getMethod(),
                        e.getReference(), e.getNote(), reversed.contains(e.getId()), e.getCreatedAt()))
                .toList();
        long balance = lines.balanceOf(party.getId());
        return new Statement(balance, settings.debtLimitCop(), balance > settings.debtLimitCop(),
                party.getRole() == Role.COURIER
                        ? settings.courierCommissionBps() : settings.merchantCommissionBps(),
                movements, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    /** Cuanto movio la deuda: una reversion deshace pagos y ajustes, asi que la aumenta. */
    private static int effect(LedgerEntry entry) {
        return entry.getKind() == Kind.COMMISSION || entry.getKind() == Kind.REVERSAL
                ? entry.getAmountCop() : -entry.getAmountCop();
    }

    private void post(LedgerEntry entry, Account debit, Account credit) {
        entries.save(entry);
        lines.saveAll(List.of(
                new LedgerLine(entry.getId(), debit, entry.getPartyId(), entry.getAmountCop(), 0),
                new LedgerLine(entry.getId(), credit, entry.getPartyId(), 0, entry.getAmountCop())));
        entries.flush();
    }

    private void requireWithinBalance(UUID partyId, int amountCop) {
        if (amountCop > lines.balanceOf(partyId)) {
            throw new ApiException(HttpStatus.CONFLICT, "AMOUNT_ABOVE_BALANCE",
                    "El valor supera la deuda pendiente");
        }
    }

    private User lockParty(UUID partyId) {
        return requireParty(users.lockById(partyId).orElse(null));
    }

    private static User requireParty(User user) {
        if (user == null || (user.getRole() != Role.RESTAURANT && user.getRole() != Role.STORE
                && user.getRole() != Role.COURIER)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Esa cuenta no tiene comisiones");
        }
        return user;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
