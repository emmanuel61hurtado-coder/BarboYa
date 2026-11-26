package com.barboya.orders;

import com.barboya.businesses.Business;
import com.barboya.businesses.BusinessService;
import com.barboya.common.ApiException;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.BiConsumer;

/** Lo que hace el comercio con los pedidos que recibe. */
@Service
@Transactional
public class MerchantOrderService {

    private final OrderRepository orders;
    private final BusinessService businesses;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;

    MerchantOrderService(OrderRepository orders, BusinessService businesses, OrderLifecycle lifecycle,
                         OrderViews views) {
        this.orders = orders;
        this.businesses = businesses;
        this.lifecycle = lifecycle;
        this.views = views;
    }

    @Transactional(readOnly = true)
    public OrderPage list(UUID ownerId, boolean finished, int page, int size) {
        Business business = businesses.requireOwned(ownerId);
        var result = orders.findByBusinessIdAndStatusInOrderByCreatedAtDesc(business.getId(),
                finished ? OrderStatus.FINISHED : OrderStatus.ACTIVE,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        return new OrderPage(views.views(result.getContent(), OrderViews.Viewer.MERCHANT, lifecycle.maxPinAttempts()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public OrderResponse accept(UUID ownerId, UUID orderId, int prepMinutes) {
        businesses.requireActiveAccount(ownerId);
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.PLACED);
            order.setPrepMinutes(prepMinutes);
            lifecycle.move(order, OrderStatus.PREPARING, ownerId, ownerId,
                    "Tiempo estimado: " + prepMinutes + " minutos");
        });
    }

    public OrderResponse reject(UUID ownerId, UUID orderId, String reason) {
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.PLACED);
            lifecycle.releaseStock(order);
            lifecycle.move(order, OrderStatus.REJECTED, ownerId, ownerId, reason.trim());
        });
    }

    public OrderResponse ready(UUID ownerId, UUID orderId) {
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.PREPARING);
            lifecycle.move(order, OrderStatus.READY, ownerId, ownerId, null);
        });
    }

    public OrderResponse dispatch(UUID ownerId, UUID orderId) {
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.READY);
            if (order.getFulfillment() != CustomerOrder.Fulfillment.DELIVERY) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                        "Este pedido no lo envia el comercio");
            }
            lifecycle.move(order, OrderStatus.OUT_FOR_DELIVERY, ownerId, ownerId, null);
        });
    }

    public OrderResponse cancel(UUID ownerId, UUID orderId, String reason) {
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY);
            if (order.getFulfillment() == CustomerOrder.Fulfillment.COURIER
                    && order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
                throw new ApiException(HttpStatus.CONFLICT, "ALREADY_PICKED_UP",
                        "El domiciliario ya lo recogio");
            }
            lifecycle.releaseStock(order);
            lifecycle.move(order, OrderStatus.CANCELLED, ownerId, ownerId, reason.trim());
        });
    }

    /** El comercio puede pedir otro domiciliario si el asignado no llega. */
    public OrderResponse unassign(UUID ownerId, UUID orderId) {
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.PREPARING, OrderStatus.READY);
            if (order.getCourierId() == null) {
                throw new ApiException(HttpStatus.CONFLICT, "NO_COURIER", "El pedido no tiene domiciliario");
            }
            lifecycle.unassign(order, ownerId, ownerId, "El comercio pidio otro domiciliario");
        });
    }

    @Transactional(readOnly = true)
    public OrderDtos.Evidence evidence(UUID ownerId, UUID orderId) {
        Business business = businesses.requireOwned(ownerId);
        return views.evidence(orders.findById(orderId)
                .filter(o -> o.getBusinessId().equals(business.getId()))
                .orElseThrow(OrderLifecycle::notFound));
    }

    /**
     * Entregas que hace el propio comercio o recogidas en el local.
     * {@code noRollbackFor}: los intentos fallidos quedan contados.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public OrderResponse deliver(UUID ownerId, UUID orderId, String pin) {
        businesses.requireActiveAccount(ownerId);
        return change(ownerId, orderId, (order, business) -> {
            if (order.getFulfillment() == CustomerOrder.Fulfillment.COURIER) {
                throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                        "La entrega la confirma el domiciliario");
            }
            lifecycle.require(order, order.getFulfillment() == CustomerOrder.Fulfillment.DELIVERY
                    ? OrderStatus.OUT_FOR_DELIVERY : OrderStatus.READY);
            lifecycle.confirmDelivery(order, ownerId, ownerId, pin);
        });
    }

    private OrderResponse change(UUID ownerId, UUID orderId, BiConsumer<CustomerOrder, Business> action) {
        Business business = businesses.requireOwned(ownerId);
        CustomerOrder order = orders.lockById(orderId)
                .filter(o -> o.getBusinessId().equals(business.getId()))
                .orElseThrow(OrderLifecycle::notFound);
        action.accept(order, business);
        return views.view(orders.saveAndFlush(order), OrderViews.Viewer.MERCHANT, lifecycle.maxPinAttempts());
    }
}
