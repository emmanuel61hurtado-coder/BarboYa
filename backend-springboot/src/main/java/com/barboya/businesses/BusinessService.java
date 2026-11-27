package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.BusinessRequest;
import com.barboya.businesses.BusinessDtos.BusinessResponse;
import com.barboya.businesses.BusinessDtos.MenuItem;
import com.barboya.businesses.BusinessDtos.PageResponse;
import com.barboya.businesses.BusinessDtos.ProductRequest;
import com.barboya.businesses.BusinessDtos.ProductResponse;
import com.barboya.businesses.BusinessDtos.PromotionRequest;
import com.barboya.businesses.BusinessDtos.Store;
import com.barboya.businesses.BusinessDtos.StoreSummary;
import com.barboya.common.ApiException;
import com.barboya.common.Colombia;
import com.barboya.common.Contacts;
import com.barboya.finance.LedgerService;
import com.barboya.products.Product;
import com.barboya.products.ProductRepository;
import com.barboya.users.UserRepository;
import com.barboya.users.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class BusinessService {

    private static final int MAX_PROMOTION_DAYS = 90;

    private final BusinessRepository businesses;
    private final ProductRepository products;
    private final UserRepository users;
    private final LedgerService ledger;
    private final Clock clock;

    public BusinessService(BusinessRepository businesses, ProductRepository products, UserRepository users,
                           LedgerService ledger, Clock clock) {
        this.ledger = ledger;
        this.businesses = businesses;
        this.products = products;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BusinessResponse owned(UUID ownerId) {
        return BusinessResponse.from(requireOwned(ownerId));
    }

    /** Se puede completar mientras la cuenta esta en revision. */
    public BusinessResponse save(UUID ownerId, BusinessRequest request) {
        boolean couriers = Boolean.TRUE.equals(request.courierDeliveryEnabled());
        if (!request.pickupEnabled() && !request.deliveryEnabled() && !couriers) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_FULFILLMENT",
                    "Activa al menos una forma de entrega");
        }
        boolean located = request.latitude() != null && request.longitude() != null;
        if (couriers && !located) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LOCATION_REQUIRED",
                    "Marca la ubicacion del comercio para usar domiciliarios");
        }
        String phone = Contacts.phone(request.phone());
        if (!phone.matches("^\\+\\d{10,15}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE",
                    "Escribe el telefono con sus 10 digitos");
        }
        Business business = businesses.findByOwnerId(ownerId)
                .orElseGet(() -> Business.create(ownerId, clock.instant()));
        business.setName(request.name().trim());
        business.setLegalName(blankToNull(request.legalName()));
        business.setNit(blankToNull(request.nit()));
        business.setCategory(request.category());
        business.setDescription(blankToNull(request.description()));
        business.setAddress(request.address().trim());
        business.setPhone(phone);
        business.setPickupEnabled(request.pickupEnabled());
        business.setDeliveryEnabled(request.deliveryEnabled());
        business.setDeliveryFeeCop(request.deliveryEnabled() ? request.deliveryFeeCop() : 0);
        business.setCourierDeliveryEnabled(couriers);
        business.setLatitude(located ? request.latitude() : null);
        business.setLongitude(located ? request.longitude() : null);
        business.setUpdatedAt(clock.instant());
        return BusinessResponse.from(businesses.saveAndFlush(business));
    }

    @Transactional(readOnly = true)
    public Business approved(UUID businessId) {
        return businesses.findById(businessId)
                .filter(b -> isApproved(b.getOwnerId()))
                .orElseThrow(BusinessService::notFound);
    }

    public BusinessResponse setAccepting(UUID ownerId, boolean accepting) {
        if (accepting) {
            requireActiveAccount(ownerId);
            if (ledger.isOverLimit(ownerId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "DEBT_LIMIT",
                        "Superaste el tope de comisiones pendientes. Ponte al dia para recibir pedidos");
            }
        }
        Business business = requireOwned(ownerId);
        business.setAcceptingOrders(accepting);
        business.setUpdatedAt(clock.instant());
        return BusinessResponse.from(businesses.saveAndFlush(business));
    }

    public BusinessResponse setHours(UUID ownerId, List<OpeningHours> week) {
        Business business = requireOwned(ownerId);
        business.replaceHours(week);
        business.setUpdatedAt(clock.instant());
        return BusinessResponse.from(businesses.saveAndFlush(business));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> ownedProducts(UUID ownerId) {
        LocalDate today = Colombia.today(clock);
        return products.findByBusinessIdAndArchivedFalseOrderByCategoryAscNameAsc(requireOwned(ownerId).getId())
                .stream().map(p -> ProductResponse.from(p, today)).toList();
    }

    public ProductResponse setPromotion(UUID ownerId, UUID productId, PromotionRequest request) {
        Product product = requireProduct(ownerId, productId);
        LocalDate today = Colombia.today(clock);
        if (request.priceCop() >= product.getPriceCop()) {
            throw invalidPromotion("El precio de la promocion debe ser menor que el precio normal");
        }
        if (request.endsOn().isBefore(request.startsOn()) || request.endsOn().isBefore(today)) {
            throw invalidPromotion("Revisa las fechas: la promocion debe terminar hoy o despues");
        }
        if (request.endsOn().isAfter(today.plusDays(MAX_PROMOTION_DAYS))) {
            throw invalidPromotion("La promocion puede terminar maximo en 90 dias");
        }
        product.promote(request.priceCop(), request.startsOn(), request.endsOn());
        product.setUpdatedAt(clock.instant());
        return ProductResponse.from(products.saveAndFlush(product), today);
    }

    public ProductResponse removePromotion(UUID ownerId, UUID productId) {
        Product product = requireProduct(ownerId, productId);
        product.endPromotion();
        product.setUpdatedAt(clock.instant());
        return ProductResponse.from(products.saveAndFlush(product), Colombia.today(clock));
    }

    public ProductResponse addProduct(UUID ownerId, ProductRequest request) {
        Product product = Product.create(requireOwned(ownerId).getId(), clock.instant());
        apply(product, request);
        return ProductResponse.from(products.saveAndFlush(product), Colombia.today(clock));
    }

    public ProductResponse updateProduct(UUID ownerId, UUID productId, ProductRequest request) {
        Product product = requireProduct(ownerId, productId);
        LocalDate today = Colombia.today(clock);
        if (product.getPromoPriceCop() != null) {
            if (product.getPromoEndsOn().isBefore(today)) {
                product.endPromotion();
            } else if (request.priceCop() <= product.getPromoPriceCop()) {
                throw new ApiException(HttpStatus.CONFLICT, "PROMOTION_CONFLICT",
                        "El precio debe ser mayor que el de la promocion; quitala o cambiala primero");
            }
        }
        apply(product, request);
        return ProductResponse.from(products.saveAndFlush(product), today);
    }


    @Transactional(readOnly = true)
    public PageResponse<StoreSummary> search(String text, Business.Category category, int page, int size) {
        String term = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        // Los comodines escritos por el usuario se buscan como texto normal
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var result = businesses.search(pattern, category,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        // Con la deuda por encima del tope se muestra cerrado
        var blocked = ledger.overLimit(result.map(Business::getOwnerId).getContent());
        Instant now = clock.instant();
        return new PageResponse<>(
                result.map(b -> StoreSummary.from(b, b.isAcceptingOrders() && b.isOpenAt(now)
                        && !blocked.contains(b.getOwnerId()))).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Store store(UUID businessId) {
        Business business = businesses.findById(businessId)
                .filter(b -> isApproved(b.getOwnerId()))
                .orElseThrow(BusinessService::notFound);
        LocalDate today = Colombia.today(clock);
        List<MenuItem> menu = products.findByBusinessIdAndArchivedFalseOrderByCategoryAscNameAsc(businessId)
                .stream().filter(Product::isAvailable).map(p -> MenuItem.from(p, today)).toList();
        boolean open = business.isAcceptingOrders() && business.isOpenAt(clock.instant())
                && !ledger.isOverLimit(business.getOwnerId());
        return new Store(StoreSummary.from(business, open), business.schedule(), menu);
    }

    public Business requireOwned(UUID ownerId) {
        return businesses.findByOwnerId(ownerId).orElseThrow(BusinessService::notFound);
    }

    public boolean isApproved(UUID ownerId) {
        return users.findById(ownerId).filter(u -> u.getStatus() == UserStatus.ACTIVE).isPresent();
    }

    public void requireActiveAccount(UUID ownerId) {
        if (!isApproved(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_APPROVED",
                    "Tu cuenta debe estar aprobada para operar");
        }
    }

    Product requireProduct(UUID ownerId, UUID productId) {
        return products.lockOwned(productId, requireOwned(ownerId).getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Producto no encontrado"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setCategory(request.category().trim());
        product.setPriceCop(request.priceCop());
        product.setStock(request.stock());
        product.setAvailable(request.available());
        product.setUpdatedAt(clock.instant());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ApiException invalidPromotion(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION", message);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "BUSINESS_NOT_FOUND", "Comercio no encontrado");
    }
}
