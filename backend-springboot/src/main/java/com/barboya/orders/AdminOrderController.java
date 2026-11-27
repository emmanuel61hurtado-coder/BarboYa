package com.barboya.orders;

import com.barboya.common.ClientInfo;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.ReasonRequest;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminOrderController {

    private final AdminOrderService service;

    public AdminOrderController(AdminOrderService service) {
        this.service = service;
    }

    @GetMapping
    public OrderPage search(@RequestParam(required = false) Long number,
                            @RequestParam(defaultValue = "false") boolean finished,
                            @RequestParam(defaultValue = "0") int page) {
        return service.search(number, finished, page);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/close")
    public OrderResponse close(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID id,
                               @Valid @RequestBody ReasonRequest body, HttpServletRequest request) {
        return service.close(actor.id(), id, body.reason(), ClientInfo.from(request));
    }
}
