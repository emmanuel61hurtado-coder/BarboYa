package com.barboya.orders;

import com.barboya.businesses.Business;
import com.barboya.businesses.BusinessRepository;
import com.barboya.businesses.BusinessService;
import com.barboya.businesses.DeliveryPricing;
import com.barboya.common.ApiException;
import com.barboya.common.Geo;
import com.barboya.config.FinanceProperties;
import com.barboya.finance.LedgerService;
import com.barboya.orders.OrderDtos.LineRequest;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.PlaceOrderRequest;
import com.barboya.products.Product;
import com.barboya.products.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lo que hace el cliente con sus pedidos. */
@Service
@Transactional
public class CustomerOrderService {

    private static final long MAX_TOTAL_COP = 20_000_000;

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final BusinessRepository businesses;
    private final BusinessService businessService;
    private final ProductRepository products;
    private final DeliveryPricing pricing;
    private final LedgerService ledger;
    private final FinanceProperties finance;
    private final OrderLifecycle lifecycle;
    private final OrderViews views;
    private final Clock clock;

    CustomerOrderService(OrderRepository orders, OrderItemRepository items, BusinessRepository businesses,
                         BusinessService businessService, ProductRepository products, DeliveryPricing pricing,
                         OrderLifecycle lifecycle, OrderViews views, LedgerService ledger,
                         FinanceProperties finance, Clock clock) {
        this.ledger = ledger;
        this.finance = finance;
        this.pricing = pricing;
        this.orders = orders;
        this.items = items;
        this.businesses = businesses;
        this.businessService = businessService;
        this.products = products;
        this.lifecycle = lifecycle;
        this.views = views;
        this.clock = clock;
    }

    /**
     * Crea el pedido con los precios del servidor y reserva existencias. Todo o
     * nada: cualquier rechazo deshace la reserva.
     */
    public OrderResponse place(UUID customerId, String idempotencyKey, PlaceOrderRequest request) {
        var repeated = orders.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (repeated.isPresent()) {
            return view(repeated.get());
        }

        Business business = businesses.findById(request.businessId())
                .filter(b -> businessService.isApproved(b.getOwnerId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BUSINESS_NOT_FOUND",
                        "Comercio no encontrado"));
        if (!business.isAcceptingOrders() || ledger.isOverLimit(business.getOwnerId())) {
            throw conflict("BUSINESS_CLOSED", "El comercio no esta recibiendo pedidos");
        }
        if (!business.isOpenAt(clock.instant())) {
            throw conflict("OUTSIDE_HOURS", "El comercio esta cerrado a esta hora");
        }
        boolean offered = switch (request.fulfillment()) {
            case PICKUP -> business.isPickupEnabled();
            case DELIVERY -> business.isDeliveryEnabled();
            case COURIER -> business.isCourierDeliveryEnabled();
        };
        if (!offered) {
            throw conflict("FULFILLMENT_UNAVAILABLE", "El comercio no ofrece esa forma de entrega");
        }
        boolean delivery = request.fulfillment() != CustomerOrder.Fulfillment.PICKUP;
        DeliveryPricing.Quote quote = null;
        if (request.fulfillment() == CustomerOrder.Fulfillment.COURIER) {
            if (request.deliveryLatitude() == null || request.deliveryLongitude() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "LOCATION_REQUIRED",
                        "Marca en el mapa el punto de entrega");
            }
            quote = pricing.quote(business,
                    new Geo.Point(request.deliveryLatitude(), request.deliveryLongitude()));
        }
        String address = request.deliveryAddress() == null ? "" : request.deliveryAddress().trim();
        if (delivery && address.length() < 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADDRESS_REQUIRED", "Escribe la direccion de entrega");
        }

        List<UUID> productIds = request.items().stream().map(LineRequest::productId).toList();
        if (productIds.stream().distinct().count() != productIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_PRODUCT",
                    "Cada producto debe aparecer una sola vez");
        }
        Map<UUID, Product> catalog = products.lockAll(productIds).stream()
                .filter(p -> p.getBusinessId().equals(business.getId()))
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Instant now = clock.instant();
        CustomerOrder order = CustomerOrder.create(customerId, business.getId(), idempotencyKey, now);
        List<OrderItem> lines = new ArrayList<>();
        long subtotal = 0;
        for (LineRequest line : request.items()) {
            Product product = catalog.get(line.productId());
            if (product == null || product.isArchived() || !product.isAvailable()) {
                throw conflict("PRODUCT_UNAVAILABLE", "Un producto del carrito ya no esta disponible");
            }
            if (!product.canSell(line.quantity())) {
                throw conflict("OUT_OF_STOCK", "No hay existencias suficientes de " + product.getName());
            }
            product.take(line.quantity());
            product.setUpdatedAt(now);
            subtotal += (long) product.getPriceCop() * line.quantity();
            if (subtotal > MAX_TOTAL_COP) {
                throw conflict("ORDER_TOO_LARGE", "El pedido supera el valor maximo permitido");
            }
            lines.add(new OrderItem(order.getId(), product.getId(), product.getName(), product.getPriceCop(),
                    line.quantity()));
        }

        int fee = quote != null ? quote.feeCop() : delivery ? business.getDeliveryFeeCop() : 0;
        int total = Math.toIntExact(subtotal) + fee;
        if (total != request.expectedTotalCop()) {
            throw conflict("PRICE_CHANGED", "Los precios cambiaron; revisa el total antes de confirmar");
        }

        order.setFulfillment(request.fulfillment());
        order.setDeliveryAddress(delivery ? address : null);
        order.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
        order.setSubtotalCop(Math.toIntExact(subtotal));
        order.setDeliveryFeeCop(fee);
        order.setTotalCop(total);
        order.setMerchantCommissionCop(finance.merchantCommission(order.getSubtotalCop()));
        order.setCourierCommissionCop(quote == null ? 0 : finance.courierCommission(fee));
        order.setDeliveryPin(lifecycle.newPin());
        if (quote != null) {
            order.setDeliveryLatitude(request.deliveryLatitude());
            order.setDeliveryLongitude(request.deliveryLongitude());
            order.setDistanceKm(quote.distanceKm());
        }
        CustomerOrder saved = orders.saveAndFlush(order);
        items.saveAll(lines);
        lifecycle.record(saved, business.getOwnerId(), customerId, null);
        return view(saved);
    }

    @Transactional(readOnly = true)
    public OrderPage list(UUID customerId, int page, int size) {
        var result = orders.findByCustomerIdOrderByCreatedAtDesc(customerId,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        return new OrderPage(views.views(result.getContent(), OrderViews.Viewer.CUSTOMER, lifecycle.maxPinAttempts()),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID customerId, UUID orderId) {
        return view(own(orders.findById(orderId).orElse(null), customerId));
    }

    @Transactional(readOnly = true)
    public OrderDtos.Evidence evidence(UUID customerId, UUID orderId) {
        return views.evidence(own(orders.findById(orderId).orElse(null), customerId));
    }

    /** Solo antes de que el comercio empiece a prepararlo. */
    public OrderResponse cancel(UUID customerId, UUID orderId) {
        CustomerOrder order = own(orders.lockById(orderId).orElse(null), customerId);
        lifecycle.require(order, OrderStatus.PLACED);
        lifecycle.releaseStock(order);
        lifecycle.move(order, OrderStatus.CANCELLED, merchantOf(order), customerId, "Cancelado por el cliente");
        return view(order);
    }

    /** Genera otro PIN y desbloquea los intentos. */
    public OrderResponse renewPin(UUID customerId, UUID orderId) {
        CustomerOrder order = own(orders.lockById(orderId).orElse(null), customerId);
        lifecycle.require(order, OrderStatus.ACTIVE.toArray(OrderStatus[]::new));
        order.setDeliveryPin(lifecycle.newPin());
        order.setPinAttempts(0);
        lifecycle.record(order, merchantOf(order), customerId, "El cliente genero un PIN nuevo");
        return view(order);
    }

    private OrderResponse view(CustomerOrder order) {
        return views.view(order, OrderViews.Viewer.CUSTOMER, lifecycle.maxPinAttempts());
    }

    private UUID merchantOf(CustomerOrder order) {
        return businesses.findById(order.getBusinessId()).orElseThrow().getOwnerId();
    }

    /** Un pedido ajeno responde igual que uno inexistente. */
    private static CustomerOrder own(CustomerOrder order, UUID customerId) {
        if (order == null || !order.getCustomerId().equals(customerId)) {
            throw OrderLifecycle.notFound();
        }
        return order;
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
