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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
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
    private final Clock clock;

    MerchantOrderService(OrderRepository orders, BusinessService businesses, OrderLifecycle lifecycle,
                         OrderViews views, Clock clock) {
        this.orders = orders;
        this.businesses = businesses;
        this.lifecycle = lifecycle;
        this.views = views;
        this.clock = clock;
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
                        "El cliente recoge este pedido");
            }
            lifecycle.move(order, OrderStatus.OUT_FOR_DELIVERY, ownerId, ownerId, null);
        });
    }

    public OrderResponse cancel(UUID ownerId, UUID orderId, String reason) {
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY);
            lifecycle.releaseStock(order);
            lifecycle.move(order, OrderStatus.CANCELLED, ownerId, ownerId, reason.trim());
        });
    }

    /**
     * Solo el PIN del cliente marca la entrega. {@code noRollbackFor}: los
     * intentos fallidos quedan contados aunque se responda con error.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public OrderResponse deliver(UUID ownerId, UUID orderId, String pin) {
        businesses.requireActiveAccount(ownerId);
        return change(ownerId, orderId, (order, business) -> {
            lifecycle.require(order, order.getFulfillment() == CustomerOrder.Fulfillment.DELIVERY
                    ? OrderStatus.OUT_FOR_DELIVERY : OrderStatus.READY);
            if (order.getPinAttempts() >= lifecycle.maxPinAttempts()) {
                throw new ApiException(HttpStatus.CONFLICT, "PIN_LOCKED",
                        "Demasiados intentos. Pide al cliente que genere un PIN nuevo");
            }
            boolean matches = MessageDigest.isEqual(order.getDeliveryPin().getBytes(StandardCharsets.US_ASCII),
                    pin.getBytes(StandardCharsets.US_ASCII));
            if (!matches) {
                order.setPinAttempts(order.getPinAttempts() + 1);
                throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PIN", "PIN incorrecto");
            }
            order.setDeliveredAt(clock.instant());
            lifecycle.move(order, OrderStatus.DELIVERED, ownerId, ownerId, "Entrega confirmada con PIN");
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
