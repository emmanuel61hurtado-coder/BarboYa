package com.barboya.orders;

import com.barboya.audit.AuditAction;
import com.barboya.audit.AuditService;
import com.barboya.businesses.BusinessRepository;
import com.barboya.common.ClientInfo;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Consulta y cierre de pedidos por el equipo, para resolver casos atascados. */
@Service
@Transactional
public class AdminOrderService {

    private final OrderRepository orders;
    private final BusinessRepository businesses;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final AuditService audit;

    AdminOrderService(OrderRepository orders, BusinessRepository businesses, OrderLifecycle lifecycle,
                      OrderViews views, AuditService audit) {
        this.orders = orders;
        this.businesses = businesses;
        this.lifecycle = lifecycle;
        this.views = views;
        this.audit = audit;
    }

    /** @param number si se indica, busca ese pedido y ignora el resto de filtros */
    @Transactional(readOnly = true)
    public OrderPage search(Long number, boolean finished, int page) {
        var request = PageRequest.of(Math.max(page, 0), 20, Sort.by("createdAt").descending());
        var result = number != null
                ? orders.findByNumber(number, request)
                : orders.findByStatusIn(finished ? OrderStatus.FINISHED : OrderStatus.ACTIVE, request);
        return new OrderPage(views.views(result.getContent(), OrderViews.Viewer.STAFF, lifecycle.maxPinAttempts()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID orderId) {
        return view(orders.findById(orderId).orElseThrow(OrderLifecycle::notFound));
    }

    /**
     * Si nadie recogio el pedido se cancela y vuelven las existencias; si ya salio,
     * queda como entrega fallida. No genera comision.
     */
    public OrderResponse close(UUID actorId, UUID orderId, String reason, ClientInfo client) {
        CustomerOrder order = orders.lockById(orderId).orElseThrow(OrderLifecycle::notFound);
        lifecycle.require(order, OrderStatus.ACTIVE.toArray(OrderStatus[]::new));
        boolean left = order.getStatus() == OrderStatus.OUT_FOR_DELIVERY;
        if (!left) {
            lifecycle.releaseStock(order);
        }
        UUID merchant = businesses.findById(order.getBusinessId()).orElseThrow().getOwnerId();
        lifecycle.move(order, left ? OrderStatus.FAILED : OrderStatus.CANCELLED, merchant, actorId,
                "Cerrado por soporte: " + reason.trim());
        audit.record(AuditAction.ORDER_CLOSED_BY_STAFF, actorId, order.getCustomerId(),
                "pedido " + order.getNumber() + " " + order.getStatus() + ": " + reason.trim(), client);
        return view(orders.saveAndFlush(order));
    }

    private OrderResponse view(CustomerOrder order) {
        return views.view(order, OrderViews.Viewer.STAFF, lifecycle.maxPinAttempts());
    }
}
