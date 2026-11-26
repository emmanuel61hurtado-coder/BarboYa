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
import com.barboya.couriers.CourierService;
import com.barboya.couriers.VehicleType;
import com.barboya.files.FileStorage;
import com.barboya.files.Uploads;
import com.barboya.orders.OrderDtos.DeliveryOffer;
import com.barboya.orders.OrderDtos.Earnings;
import com.barboya.orders.OrderDtos.Evidence;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lo que hace un domiciliario: ver pedidos cercanos, tomar uno y entregarlo. */
@Service
@Transactional
public class CourierDeliveryService {

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final OrderRepository orders;
    private final BusinessRepository businesses;
    private final CourierService couriers;
    private final CourierProfileRepository profiles;
    private final CourierPresenceRepository presence;
    private final UserRepository users;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final FileStorage storage;
    private final DeliveryProperties settings;
    private final Clock clock;

    CourierDeliveryService(OrderRepository orders, BusinessRepository businesses, CourierService couriers,
                           CourierProfileRepository profiles, CourierPresenceRepository presence,
                           UserRepository users, OrderLifecycle lifecycle, OrderViews views, FileStorage storage,
                           DeliveryProperties settings, Clock clock) {
        this.orders = orders;
        this.businesses = businesses;
        this.couriers = couriers;
        this.profiles = profiles;
        this.presence = presence;
        this.users = users;
        this.lifecycle = lifecycle;
        this.views = views;
        this.storage = storage;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Pedidos que esta persona puede tomar ahora, del mas cercano al mas lejano.
     * No incluyen datos del cliente: se revelan al tomar el pedido.
     */
    @Transactional(readOnly = true)
    public List<DeliveryOffer> offers(UUID courierId) {
        couriers.requireOperable(courierId);
        if (hasActive(courierId)) {
            return List.of();
        }
        Geo.Point position = position(courierId);
        VehicleType vehicle = profiles.findById(courierId).orElseThrow().getVehicleType();
        List<CustomerOrder> open = orders.findByFulfillmentAndCourierIdIsNullAndStatusIn(
                CustomerOrder.Fulfillment.COURIER, List.of(OrderStatus.PREPARING, OrderStatus.READY));
        Map<UUID, Business> stores = businesses
                .findAllById(open.stream().map(CustomerOrder::getBusinessId).distinct().toList()).stream()
                .collect(Collectors.toMap(Business::getId, Function.identity()));
        return open.stream()
                .map(order -> offer(order, stores.get(order.getBusinessId()), position, vehicle))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(DeliveryOffer::pickupDistanceKm))
                .limit(20)
                .toList();
    }

    public OrderResponse accept(UUID courierId, UUID orderId) {
        // Bloquear a la persona evita que tome dos pedidos a la vez
        users.lockById(courierId).orElseThrow(OrderLifecycle::notFound);
        couriers.requireOperable(courierId);
        if (hasActive(courierId)) {
            throw conflict("ALREADY_DELIVERING", "Termina tu entrega actual antes de tomar otra");
        }
        Geo.Point position = position(courierId);
        VehicleType vehicle = profiles.findById(courierId).orElseThrow().getVehicleType();
        CustomerOrder order = orders.lockById(orderId)
                .filter(o -> o.getFulfillment() == CustomerOrder.Fulfillment.COURIER)
                .orElseThrow(OrderLifecycle::notFound);
        if (order.getCourierId() != null
                || (order.getStatus() != OrderStatus.PREPARING && order.getStatus() != OrderStatus.READY)) {
            throw conflict("OFFER_TAKEN", "Ese pedido ya no esta disponible");
        }
        Business business = businesses.findById(order.getBusinessId()).orElseThrow();
        if (offer(order, business, position, vehicle) == null) {
            throw conflict("OFFER_NOT_ELIGIBLE", "Ese pedido esta fuera de tu alcance");
        }
        order.setCourierId(courierId);
        order.setAssignedAt(clock.instant());
        order.setPickupCode(lifecycle.newPin());
        order.setPickupAttempts(0);
        lifecycle.record(order, business.getOwnerId(), courierId, "Domiciliario asignado");
        return view(orders.saveAndFlush(order));
    }

    /** @return la entrega en curso, o {@code null} */
    @Transactional(readOnly = true)
    public OrderResponse current(UUID courierId) {
        return orders.findFirstByCourierIdAndStatusIn(courierId, OrderStatus.ACTIVE).map(this::view).orElse(null);
    }

    /** Antes de recoger se puede soltar el pedido para que lo tome otra persona. */
    public void release(UUID courierId, UUID orderId, String reason) {
        CustomerOrder order = own(courierId, orderId);
        lifecycle.require(order, OrderStatus.PREPARING, OrderStatus.READY);
        lifecycle.unassign(order, merchantOf(order), courierId, "El domiciliario solto el pedido: " + reason.trim());
    }

    /** El codigo lo entrega el comercio: prueba que el pedido cambio de manos. */
    @Transactional(noRollbackFor = ApiException.class)
    public OrderResponse pickUp(UUID courierId, UUID orderId, String code) {
        couriers.requireOperable(courierId);
        CustomerOrder order = own(courierId, orderId);
        lifecycle.require(order, OrderStatus.READY);
        if (order.getPickupAttempts() >= lifecycle.maxPinAttempts()) {
            throw conflict("CODE_LOCKED", "Demasiados intentos. Suelta el pedido o pide ayuda al comercio");
        }
        if (!lifecycle.matches(order.getPickupCode(), code)) {
            order.setPickupAttempts(order.getPickupAttempts() + 1);
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_CODE", "Codigo de recogida incorrecto");
        }
        order.setPickedUpAt(clock.instant());
        lifecycle.move(order, OrderStatus.OUT_FOR_DELIVERY, merchantOf(order), courierId,
                "Recogido por el domiciliario");
        return view(orders.saveAndFlush(order));
    }

    public OrderResponse addEvidence(UUID courierId, UUID orderId, MultipartFile file) {
        CustomerOrder order = own(courierId, orderId);
        lifecycle.require(order, OrderStatus.OUT_FOR_DELIVERY);
        var upload = Uploads.read(file, false);
        String previous = order.getEvidenceKey();
        String key = storage.store(upload.content());
        order.setEvidenceKey(key);
        order.setEvidenceType(upload.contentType());
        cleanUp(previous, key);
        lifecycle.record(order, merchantOf(order), courierId, "Foto de entrega adjuntada");
        return view(orders.saveAndFlush(order));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public OrderResponse deliver(UUID courierId, UUID orderId, String pin) {
        CustomerOrder order = own(courierId, orderId);
        lifecycle.require(order, OrderStatus.OUT_FOR_DELIVERY);
        lifecycle.confirmDelivery(order, merchantOf(order), courierId, pin);
        return view(orders.saveAndFlush(order));
    }

    /** Ya recogido y sin poder entregar: queda registrado para resolverlo con soporte. */
    public OrderResponse fail(UUID courierId, UUID orderId, String reason) {
        CustomerOrder order = own(courierId, orderId);
        lifecycle.require(order, OrderStatus.OUT_FOR_DELIVERY);
        order.setStatusReason(reason.trim());
        lifecycle.move(order, OrderStatus.FAILED, merchantOf(order), courierId, reason.trim());
        return view(orders.saveAndFlush(order));
    }

    @Transactional(readOnly = true)
    public OrderPage history(UUID courierId, int page) {
        var result = orders.findByCourierIdAndStatusInOrderByCreatedAtDesc(courierId, OrderStatus.FINISHED,
                PageRequest.of(Math.max(page, 0), 20));
        return new OrderPage(views.views(result.getContent(), OrderViews.Viewer.COURIER, lifecycle.maxPinAttempts()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    /** Tarifas de entregas completadas. El dia empieza a medianoche en Colombia. */
    @Transactional(readOnly = true)
    public Earnings earnings(UUID courierId) {
        Instant today = LocalDate.ofInstant(clock.instant(), COLOMBIA).atStartOfDay(COLOMBIA).toInstant();
        return new Earnings(
                orders.earningsSince(courierId, today),
                orders.earningsSince(courierId, today.minus(Duration.ofDays(6))),
                orders.earningsSince(courierId, today.minus(Duration.ofDays(29))));
    }

    @Transactional(readOnly = true)
    public Evidence evidence(UUID courierId, UUID orderId) {
        return views.evidence(orders.findById(orderId)
                .filter(o -> courierId.equals(o.getCourierId()))
                .orElseThrow(OrderLifecycle::notFound));
    }

    private DeliveryOffer offer(CustomerOrder order, Business business, Geo.Point position, VehicleType vehicle) {
        double toPickup = Geo.distanceKm(position, new Geo.Point(business.getLatitude(), business.getLongitude()));
        boolean tooFar = toPickup > settings.pickupRadiusKm()
                || (vehicle == VehicleType.BICYCLE && toPickup + order.getDistanceKm() > settings.bicycleMaxKm());
        return tooFar ? null : new DeliveryOffer(order.getId(), order.getNumber(), order.getStatus(),
                business.getName(), business.getAddress(), Geo.round(toPickup), order.getDistanceKm(),
                order.getDeliveryFeeCop(), order.getSubtotalCop(), order.getTotalCop());
    }

    private boolean hasActive(UUID courierId) {
        return orders.findFirstByCourierIdAndStatusIn(courierId, OrderStatus.ACTIVE).isPresent();
    }

    private Geo.Point position(UUID courierId) {
        return presence.findById(courierId)
                .filter(CourierPresence::isOnline)
                .flatMap(p -> p.freshPosition(clock.instant(), settings.locationMaxAge()))
                .orElseThrow(() -> conflict("NOT_AVAILABLE",
                        "Conectate y comparte tu ubicacion para ver pedidos"));
    }

    private CustomerOrder own(UUID courierId, UUID orderId) {
        return orders.lockById(orderId)
                .filter(o -> courierId.equals(o.getCourierId()))
                .orElseThrow(OrderLifecycle::notFound);
    }

    private UUID merchantOf(CustomerOrder order) {
        return businesses.findById(order.getBusinessId()).orElseThrow().getOwnerId();
    }

    private OrderResponse view(CustomerOrder order) {
        return views.view(order, OrderViews.Viewer.COURIER, lifecycle.maxPinAttempts());
    }

    /** Se borra la foto anterior solo si la nueva queda confirmada, y al reves. */
    private void cleanUp(String previous, String added) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                String obsolete = status == STATUS_COMMITTED ? previous : added;
                if (obsolete != null) {
                    storage.delete(obsolete);
                }
            }
        });
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
