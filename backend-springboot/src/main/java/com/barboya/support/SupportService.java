package com.barboya.support;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.common.ApiException;
import com.barboya.common.ClientInfo;
import com.barboya.notifications.MailRequested;
import com.barboya.orders.DeliveryFailed;
import com.barboya.orders.OrderAccess;
import com.barboya.support.SupportDtos.Message;
import com.barboya.support.SupportDtos.NewTicket;
import com.barboya.support.SupportDtos.Opener;
import com.barboya.support.SupportDtos.TicketPage;
import com.barboya.support.SupportDtos.TicketSummary;
import com.barboya.support.SupportDtos.TicketView;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SupportService {

    private static final int MAX_OPEN = 10;

    private final TicketRepository tickets;
    private final TicketMessageRepository messages;
    private final OrderAccess orders;
    private final UserRepository users;
    private final AuditService audit;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public SupportService(TicketRepository tickets, TicketMessageRepository messages, OrderAccess orders,
                          UserRepository users, AuditService audit, ApplicationEventPublisher publisher,
                          Clock clock) {
        this.tickets = tickets;
        this.messages = messages;
        this.orders = orders;
        this.users = users;
        this.audit = audit;
        this.publisher = publisher;
        this.clock = clock;
    }

    public TicketView open(UUID userId, NewTicket request) {
        // Bloquear a la persona evita saltarse el limite con peticiones simultaneas
        users.lockById(userId).orElseThrow(SupportService::notFound);
        if (request.orderId() != null) {
            if (!orders.participates(request.orderId(), userId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Pedido no encontrado");
            }
            if (tickets.existsByOpenerIdAndOrderIdAndStatusNot(userId, request.orderId(), Ticket.Status.RESOLVED)) {
                throw new ApiException(HttpStatus.CONFLICT, "TICKET_EXISTS",
                        "Ya tienes un caso abierto sobre este pedido");
            }
        }
        if (tickets.countByOpenerIdAndStatusNot(userId, Ticket.Status.RESOLVED) >= MAX_OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "TOO_MANY_TICKETS",
                    "Tienes demasiados casos abiertos; responde en alguno de ellos");
        }
        Ticket ticket = create(userId, request.orderId(), request.category(), request.subject().trim(),
                request.message().trim());
        return view(ticket, false);
    }

    /** Una entrega fallida abre sola un caso para que el equipo la resuelva. */
    @EventListener
    public void onDeliveryFailed(DeliveryFailed event) {
        if (!tickets.existsByOpenerIdAndOrderIdAndStatusNot(event.courierId(), event.orderId(),
                Ticket.Status.RESOLVED)) {
            create(event.courierId(), event.orderId(), Ticket.Category.DELIVERY_FAILED,
                    "Entrega fallida del pedido " + event.orderNumber(), event.reason());
        }
    }

    @Transactional(readOnly = true)
    public TicketPage mine(UUID userId, int page) {
        return page(tickets.findByOpenerId(userId, request(page, Sort.by("updatedAt").descending())));
    }

    /** Los casos que esperan respuesta salen primero los mas antiguos. */
    @Transactional(readOnly = true)
    public TicketPage queue(Ticket.Status status, int page) {
        var sort = status == Ticket.Status.OPEN
                ? Sort.by("updatedAt").ascending() : Sort.by("updatedAt").descending();
        return page(tickets.findByStatusIn(Set.of(status), request(page, sort)));
    }

    @Transactional(readOnly = true)
    public TicketView own(UUID userId, UUID ticketId) {
        return view(requireOwn(userId, ticketId), false);
    }

    @Transactional(readOnly = true)
    public TicketView forStaff(UUID ticketId) {
        return view(tickets.findById(ticketId).orElseThrow(SupportService::notFound), true);
    }

    public TicketView reply(UUID userId, UUID ticketId, String body) {
        Ticket ticket = requireOwn(userId, ticketId);
        add(ticket, userId, false, body.trim());
        return view(ticket, false);
    }

    public TicketView staffReply(UUID staffId, UUID ticketId, String body) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(SupportService::notFound);
        add(ticket, staffId, true, body.trim());
        notifyOpener(ticket);
        return view(ticket, true);
    }

    public TicketView resolve(UUID staffId, UUID ticketId, String resolution, ClientInfo client) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(SupportService::notFound);
        if (ticket.getStatus() == Ticket.Status.RESOLVED) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_RESOLVED", "El caso ya esta resuelto");
        }
        Instant now = clock.instant();
        messages.save(new TicketMessage(ticket.getId(), staffId, true, resolution.trim(), now));
        ticket.resolve(resolution.trim(), staffId, now);
        audit.record(AuditAction.TICKET_RESOLVED, staffId, ticket.getOpenerId(),
                "caso " + ticket.getNumber() + ": " + resolution.trim(), client);
        notifyOpener(ticket);
        tickets.flush();
        return view(ticket, true);
    }

    private Ticket create(UUID openerId, UUID orderId, Ticket.Category category, String subject, String body) {
        Instant now = clock.instant();
        Ticket ticket = tickets.saveAndFlush(new Ticket(openerId, orderId, category, subject, now));
        messages.save(new TicketMessage(ticket.getId(), openerId, false, body, now));
        return ticket;
    }

    private void add(Ticket ticket, UUID authorId, boolean staff, String body) {
        Instant now = clock.instant();
        messages.save(new TicketMessage(ticket.getId(), authorId, staff, body, now));
        ticket.replied(staff, now);
        tickets.flush();
    }

    private void notifyOpener(Ticket ticket) {
        publisher.publishEvent(new TicketChanged(ticket.getId(), ticket.getOpenerId()));
        users.findById(ticket.getOpenerId()).ifPresent(user -> publisher.publishEvent(new MailRequested(
                user.getEmail(), "Respuesta de soporte BarboYa",
                "Hay una respuesta en tu caso " + ticket.getNumber() + ". Ingresa a BarboYa para verla.")));
    }

    private TicketView view(Ticket ticket, boolean staff) {
        User user = staff ? users.findById(ticket.getOpenerId()).orElseThrow() : null;
        List<Message> thread = messages.findByTicketIdOrderByCreatedAt(ticket.getId()).stream()
                .map(m -> new Message(m.isStaff(), m.getBody(), m.getCreatedAt()))
                .toList();
        return new TicketView(TicketSummary.from(ticket),
                user == null ? null
                        : new Opener(user.getFullName(), user.getPhone(), user.getEmail(), user.getRole().name()),
                ticket.getResolution(), thread);
    }

    private Ticket requireOwn(UUID userId, UUID ticketId) {
        return tickets.findById(ticketId)
                .filter(t -> t.getOpenerId().equals(userId))
                .orElseThrow(SupportService::notFound);
    }

    private static TicketPage page(Page<Ticket> result) {
        return new TicketPage(result.map(TicketSummary::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private static PageRequest request(int page, Sort sort) {
        return PageRequest.of(Math.max(page, 0), 20, sort);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Caso no encontrado");
    }
}
