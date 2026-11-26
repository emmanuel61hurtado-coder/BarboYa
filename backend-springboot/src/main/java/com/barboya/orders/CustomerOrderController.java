package com.barboya.orders;

import com.barboya.common.ApiException;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.PlaceOrderRequest;
import com.barboya.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final CustomerOrderService service;

    public CustomerOrderController(CustomerOrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@AuthenticationPrincipal AuthUser user,
                               @RequestHeader(name = "Idempotency-Key", required = false) String key,
                               @Valid @RequestBody PlaceOrderRequest body) {
        if (key == null || !key.matches("^[A-Za-z0-9-]{16,64}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                    "Falta el identificador de la operacion");
        }
        return service.place(user.id(), key, body);
    }

    @GetMapping
    public OrderPage list(@AuthenticationPrincipal AuthUser user,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return service.list(user.id(), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.cancel(user.id(), id);
    }

    @PostMapping("/{id}/new-pin")
    public OrderResponse newPin(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.renewPin(user.id(), id);
    }
}
