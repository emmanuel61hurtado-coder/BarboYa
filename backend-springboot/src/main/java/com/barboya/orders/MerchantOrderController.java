package com.barboya.orders;

import com.barboya.orders.OrderDtos.AcceptRequest;
import com.barboya.orders.OrderDtos.DeliverRequest;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.ReasonRequest;
import com.barboya.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/orders")
@PreAuthorize("hasAnyRole('RESTAURANT','STORE')")
public class MerchantOrderController {

    private final MerchantOrderService service;

    public MerchantOrderController(MerchantOrderService service) {
        this.service = service;
    }

    @GetMapping
    public OrderPage list(@AuthenticationPrincipal AuthUser user,
                          @RequestParam(defaultValue = "false") boolean finished,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return service.list(user.id(), finished, page, size);
    }

    @PostMapping("/{id}/accept")
    public OrderResponse accept(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                @Valid @RequestBody AcceptRequest body) {
        return service.accept(user.id(), id, body.prepMinutes());
    }

    @PostMapping("/{id}/reject")
    public OrderResponse reject(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                @Valid @RequestBody ReasonRequest body) {
        return service.reject(user.id(), id, body.reason());
    }

    @PostMapping("/{id}/ready")
    public OrderResponse ready(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.ready(user.id(), id);
    }

    @PostMapping("/{id}/dispatch")
    public OrderResponse dispatch(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.dispatch(user.id(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                @Valid @RequestBody ReasonRequest body) {
        return service.cancel(user.id(), id, body.reason());
    }

    @PostMapping("/{id}/unassign")
    public OrderResponse unassign(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.unassign(user.id(), id);
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<byte[]> evidence(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return EvidenceResponses.of(service.evidence(user.id(), id));
    }

    @PostMapping("/{id}/deliver")
    public OrderResponse deliver(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                 @Valid @RequestBody DeliverRequest body) {
        return service.deliver(user.id(), id, body.pin());
    }
}
