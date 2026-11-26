package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.AcceptingRequest;
import com.barboya.businesses.BusinessDtos.BusinessRequest;
import com.barboya.businesses.BusinessDtos.BusinessResponse;
import com.barboya.businesses.BusinessDtos.ProductRequest;
import com.barboya.businesses.BusinessDtos.ProductResponse;
import com.barboya.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** El comercio siempre es el de quien inicio sesion: no se recibe su id. */
@RestController
@RequestMapping("/api/v1/merchant")
@PreAuthorize("hasAnyRole('RESTAURANT','STORE')")
public class MerchantController {

    private final BusinessService service;

    public MerchantController(BusinessService service) {
        this.service = service;
    }

    @GetMapping("/business")
    public BusinessResponse business(@AuthenticationPrincipal AuthUser user) {
        return service.owned(user.id());
    }

    @PutMapping("/business")
    public BusinessResponse save(@AuthenticationPrincipal AuthUser user,
                                 @Valid @RequestBody BusinessRequest body) {
        return service.save(user.id(), body);
    }

    @PatchMapping("/business/accepting")
    public BusinessResponse accepting(@AuthenticationPrincipal AuthUser user,
                                      @Valid @RequestBody AcceptingRequest body) {
        return service.setAccepting(user.id(), body.accepting());
    }

    @GetMapping("/products")
    public List<ProductResponse> products(@AuthenticationPrincipal AuthUser user) {
        return service.ownedProducts(user.id());
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse add(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ProductRequest body) {
        return service.addProduct(user.id(), body);
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                  @Valid @RequestBody ProductRequest body) {
        return service.updateProduct(user.id(), id, body);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        service.archiveProduct(user.id(), id);
    }
}
