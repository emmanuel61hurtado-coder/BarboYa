package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.BusinessRequest;
import com.barboya.businesses.BusinessDtos.BusinessResponse;
import com.barboya.businesses.BusinessDtos.MenuItem;
import com.barboya.businesses.BusinessDtos.PageResponse;
import com.barboya.businesses.BusinessDtos.ProductRequest;
import com.barboya.businesses.BusinessDtos.ProductResponse;
import com.barboya.businesses.BusinessDtos.Store;
import com.barboya.businesses.BusinessDtos.StoreSummary;
import com.barboya.common.ApiException;
import com.barboya.common.Contacts;
import com.barboya.products.Product;
import com.barboya.products.ProductRepository;
import com.barboya.users.UserRepository;
import com.barboya.users.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class BusinessService {

    private final BusinessRepository businesses;
    private final ProductRepository products;
    private final UserRepository users;
    private final Clock clock;

    public BusinessService(BusinessRepository businesses, ProductRepository products, UserRepository users,
                           Clock clock) {
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
        Business business = businesses.findByOwnerId(ownerId)
                .orElseGet(() -> Business.create(ownerId, clock.instant()));
        business.setName(request.name().trim());
        business.setLegalName(blankToNull(request.legalName()));
        business.setNit(blankToNull(request.nit()));
        business.setCategory(request.category());
        business.setDescription(blankToNull(request.description()));
        business.setAddress(request.address().trim());
        business.setPhone(Contacts.phone(request.phone()));
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
        }
        Business business = requireOwned(ownerId);
        business.setAcceptingOrders(accepting);
        business.setUpdatedAt(clock.instant());
        return BusinessResponse.from(businesses.saveAndFlush(business));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> ownedProducts(UUID ownerId) {
        return products.findByBusinessIdAndArchivedFalseOrderByCategoryAscNameAsc(requireOwned(ownerId).getId())
                .stream().map(ProductResponse::from).toList();
    }

    public ProductResponse addProduct(UUID ownerId, ProductRequest request) {
        Product product = Product.create(requireOwned(ownerId).getId(), clock.instant());
        apply(product, request);
        return ProductResponse.from(products.saveAndFlush(product));
    }

    public ProductResponse updateProduct(UUID ownerId, UUID productId, ProductRequest request) {
        Product product = requireProduct(ownerId, productId);
        apply(product, request);
        return ProductResponse.from(products.saveAndFlush(product));
    }

    public void archiveProduct(UUID ownerId, UUID productId) {
        Product product = requireProduct(ownerId, productId);
        product.setArchived(true);
        product.setUpdatedAt(clock.instant());
    }

    @Transactional(readOnly = true)
    public PageResponse<StoreSummary> search(String text, Business.Category category, int page, int size) {
        String term = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        // Los comodines escritos por el usuario se buscan como texto normal
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var result = businesses.search(pattern, category,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)));
        return new PageResponse<>(result.map(StoreSummary::from).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Store store(UUID businessId) {
        Business business = businesses.findById(businessId)
                .filter(b -> isApproved(b.getOwnerId()))
                .orElseThrow(BusinessService::notFound);
        List<MenuItem> menu = products.findByBusinessIdAndArchivedFalseOrderByCategoryAscNameAsc(businessId)
                .stream().filter(Product::isAvailable).map(MenuItem::from).toList();
        return new Store(StoreSummary.from(business), menu);
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

    private Product requireProduct(UUID ownerId, UUID productId) {
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

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "BUSINESS_NOT_FOUND", "Comercio no encontrado");
    }
}
