package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.PageResponse;
import com.barboya.businesses.BusinessDtos.Store;
import com.barboya.businesses.BusinessDtos.StoreSummary;
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

    public CatalogController(BusinessService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<StoreSummary> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Business.Category category,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.search(q, category, page, size);
    }

    @GetMapping("/{id}")
    public Store store(@PathVariable UUID id) {
        return service.store(id);
    }
}
