package com.barboya.orders;

import com.barboya.businesses.Business;
import com.barboya.businesses.BusinessRepository;
import com.barboya.common.ApiException;
import com.barboya.common.Geo;
import com.barboya.config.DeliveryProperties;
import com.barboya.couriers.CourierPresence;
import com.barboya.couriers.CourierPresenceRepository;
import com.barboya.couriers.CourierProfile;
import com.barboya.couriers.CourierProfileRepository;
import com.barboya.files.FileStorage;
import com.barboya.orders.OrderDtos.Contact;
import com.barboya.orders.OrderDtos.CourierInfo;
import com.barboya.orders.OrderDtos.Evidence;
import com.barboya.orders.OrderDtos.Line;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.Step;
import com.barboya.users.User;
import com.barboya.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Construye la respuesta segun quien mira, cargando los datos por lotes. */
@Component
class OrderViews {

    enum Viewer {
        CUSTOMER,
        MERCHANT,
        COURIER
    }

    private final OrderItemRepository items;
    private final OrderEventRepository events;
    private final BusinessRepository businesses;
    private final UserRepository users;
    private final CourierProfileRepository profiles;
    private final CourierPresenceRepository presence;
    private final FileStorage storage;
    private final DeliveryProperties delivery;
    private final Clock clock;

    OrderViews(OrderItemRepository items, OrderEventRepository events, BusinessRepository businesses,
               UserRepository users, CourierProfileRepository profiles, CourierPresenceRepository presence,
               FileStorage storage, DeliveryProperties delivery, Clock clock) {
        this.items = items;
        this.events = events;
        this.businesses = businesses;
        this.users = users;
        this.profiles = profiles;
        this.presence = presence;
        this.storage = storage;
        this.delivery = delivery;
        this.clock = clock;
    }

    OrderResponse view(CustomerOrder order, Viewer viewer, int maxPinAttempts) {
        return views(List.of(order), viewer, maxPinAttempts).getFirst();
    }

    /** Quien llama ya comprobo que puede ver este pedido. */
    Evidence evidence(CustomerOrder order) {
        if (order.getEvidenceKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NO_EVIDENCE", "Este pedido no tiene foto de entrega");
        }
        return new Evidence(storage.read(order.getEvidenceKey()), order.getEvidenceType());
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
        List<UUID> courierIds = orders.stream().map(CustomerOrder::getCourierId)
                .filter(Objects::nonNull).distinct().toList();
        Map<UUID, User> people = users.findAllById(Stream.concat(courierIds.stream(),
                        orders.stream().map(CustomerOrder::getCustomerId)).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, CourierProfile> vehicles = profiles.findAllById(courierIds).stream()
                .collect(Collectors.toMap(CourierProfile::getUserId, Function.identity()));
        Map<UUID, CourierPresence> positions = presence.findAllById(courierIds).stream()
                .collect(Collectors.toMap(CourierPresence::getCourierId, Function.identity()));

        return orders.stream().map(order -> {
            Business store = stores.get(order.getBusinessId());
            boolean active = OrderStatus.ACTIVE.contains(order.getStatus());
            User customer = people.get(order.getCustomerId());
            boolean seesCustomer = active && viewer != Viewer.CUSTOMER;
            // Despues de la entrega el domiciliario deja de ver donde vive el cliente
            boolean seesDestination = viewer != Viewer.COURIER || active;
            return new OrderResponse(
                    order.getId(), order.getNumber(), order.getStatus(), order.getFulfillment(),
                    store.getId(), store.getName(), store.getPhone(), store.getAddress(),
                    point(store.getLatitude(), store.getLongitude()),
                    seesCustomer ? new Contact(customer.getFullName(), customer.getPhone()) : null,
                    seesDestination ? order.getDeliveryAddress() : null,
                    seesDestination ? point(order.getDeliveryLatitude(), order.getDeliveryLongitude()) : null,
                    order.getDistanceKm(),
                    seesDestination ? order.getNotes() : null,
                    lines.getOrDefault(order.getId(), List.of()).stream()
                            .map(i -> new Line(i.getProductId(), i.getName(), i.getUnitPriceCop(),
                                    i.getQuantity(), i.getLineTotalCop()))
                            .toList(),
                    order.getSubtotalCop(), order.getDeliveryFeeCop(), order.getTotalCop(),
                    order.getPaymentMethod(), order.getPrepMinutes(), order.getStatusReason(),
                    viewer == Viewer.CUSTOMER && active ? order.getDeliveryPin() : null,
                    active && order.getPinAttempts() >= maxPinAttempts,
                    viewer == Viewer.MERCHANT && awaitingPickup(order) ? order.getPickupCode() : null,
                    courier(order, viewer, active, people, vehicles, positions),
                    order.getEvidenceKey() != null,
                    order.getCreatedAt(), order.getDeliveredAt(),
                    history.getOrDefault(order.getId(), List.of()).stream()
                            .map(e -> new Step(e.getStatus(), e.getNote(), e.getCreatedAt()))
                            .toList());
        }).toList();
    }

    /** Nombre, vehiculo y placa para reconocerlo; la posicion solo mientras entrega. */
    private CourierInfo courier(CustomerOrder order, Viewer viewer, boolean active, Map<UUID, User> people,
                                Map<UUID, CourierProfile> vehicles, Map<UUID, CourierPresence> positions) {
        UUID id = order.getCourierId();
        if (id == null || viewer == Viewer.COURIER) {
            return null;
        }
        CourierPresence last = positions.get(id);
        Geo.Point position = !active || last == null
                ? null : last.freshPosition(clock.instant(), delivery.locationMaxAge()).orElse(null);
        User person = people.get(id);
        CourierProfile profile = vehicles.get(id);
        return new CourierInfo(person.getFullName(), active ? person.getPhone() : null,
                profile.getVehicleType(), profile.getPlate(), position);
    }

    private static boolean awaitingPickup(CustomerOrder order) {
        return order.getCourierId() != null
                && (order.getStatus() == OrderStatus.PREPARING || order.getStatus() == OrderStatus.READY);
    }

    private static Geo.Point point(Double latitude, Double longitude) {
        return latitude == null ? null : new Geo.Point(latitude, longitude);
    }
}
