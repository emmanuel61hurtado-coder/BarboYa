package com.barboya.orders;

import com.barboya.businesses.Business;
import com.barboya.businesses.BusinessRepository;
import com.barboya.orders.OrderDtos.Contact;
import com.barboya.orders.OrderDtos.Line;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.Step;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Construye la respuesta segun quien mira, cargando los datos por lotes. */
@Component
class OrderViews {

    enum Viewer {
        CUSTOMER,
        MERCHANT
    }

    private final OrderItemRepository items;
    private final OrderEventRepository events;
    private final BusinessRepository businesses;
    private final UserRepository users;

    OrderViews(OrderItemRepository items, OrderEventRepository events, BusinessRepository businesses,
               UserRepository users) {
        this.items = items;
        this.events = events;
        this.businesses = businesses;
        this.users = users;
    }

    OrderResponse view(CustomerOrder order, Viewer viewer, int maxPinAttempts) {
        return views(List.of(order), viewer, maxPinAttempts).getFirst();
    }

    List<OrderResponse> views(List<CustomerOrder> orders, Viewer viewer, int maxPinAttempts) {
        List<UUID> ids = orders.stream().map(CustomerOrder::getId).toList();
        Map<UUID, List<OrderItem>> lines = items.findByOrderIdInOrderByName(ids).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<UUID, List<OrderEvent>> history = events.findByOrderIdInOrderByCreatedAt(ids).stream()
                .collect(Collectors.groupingBy(OrderEvent::getOrderId));
        Map<UUID, Business> stores = businesses
                .findAllById(orders.stream().map(CustomerOrder::getBusinessId).distinct().toList()).stream()
                .collect(Collectors.toMap(Business::getId, Function.identity()));
        Map<UUID, User> customers = viewer == Viewer.MERCHANT
                ? users.findAllById(orders.stream().map(CustomerOrder::getCustomerId).distinct().toList()).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()))
                : Map.of();

        return orders.stream().map(order -> {
            Business store = stores.get(order.getBusinessId());
            boolean active = OrderStatus.ACTIVE.contains(order.getStatus());
            User customer = customers.get(order.getCustomerId());
            return new OrderResponse(
                    order.getId(), order.getNumber(), order.getStatus(), order.getFulfillment(),
                    store.getId(), store.getName(), store.getPhone(), store.getAddress(),
                    viewer == Viewer.MERCHANT && active
                            ? new Contact(customer.getFullName(), customer.getPhone()) : null,
                    order.getDeliveryAddress(), order.getNotes(),
                    lines.getOrDefault(order.getId(), List.of()).stream()
                            .map(i -> new Line(i.getProductId(), i.getName(), i.getUnitPriceCop(),
                                    i.getQuantity(), i.getLineTotalCop()))
                            .toList(),
                    order.getSubtotalCop(), order.getDeliveryFeeCop(), order.getTotalCop(),
                    order.getPaymentMethod(), order.getPrepMinutes(), order.getStatusReason(),
                    viewer == Viewer.CUSTOMER && active ? order.getDeliveryPin() : null,
                    active && order.getPinAttempts() >= maxPinAttempts,
                    order.getCreatedAt(), order.getDeliveredAt(),
                    history.getOrDefault(order.getId(), List.of()).stream()
                            .map(e -> new Step(e.getStatus(), e.getNote(), e.getCreatedAt()))
                            .toList());
        }).toList();
    }
}
