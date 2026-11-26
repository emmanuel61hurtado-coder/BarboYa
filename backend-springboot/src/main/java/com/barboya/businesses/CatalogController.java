package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.PageResponse;
import com.barboya.businesses.BusinessDtos.Store;
import com.barboya.businesses.BusinessDtos.StoreSummary;
import com.barboya.common.ApiException;
import com.barboya.common.Geo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/businesses")
public class CatalogController {

    private final BusinessService service;

    private final DeliveryPricing pricing;

    public CatalogController(BusinessService service, DeliveryPricing pricing) {
        this.pricing = pricing;
        this.service = service;
    }

    @GetMapping
    public PageResponse<StoreSummary> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Business.Category category,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.search(q, category, page, size);
    }

    /** Cuanto cuesta que un domiciliario lleve un pedido hasta ese punto. */
    @GetMapping("/{id}/delivery-quote")
    public DeliveryPricing.Quote quote(@PathVariable UUID id, @RequestParam double lat, @RequestParam double lng) {
        if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOCATION", "Ubicacion no valida");
        }
        return pricing.quote(service.approved(id), new Geo.Point(lat, lng));
    }

    @GetMapping("/{id}")
    public Store store(@PathVariable UUID id) {
        return service.store(id);
    }
}
