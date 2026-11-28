package com.barboya.orders;

import com.barboya.orders.OrderDtos.DeliverRequest;
import com.barboya.orders.OrderDtos.DeliveryOffer;
import com.barboya.orders.OrderDtos.Earnings;
import com.barboya.orders.OrderDtos.OrderPage;
import com.barboya.orders.OrderDtos.OrderResponse;
import com.barboya.orders.OrderDtos.PickupRequest;
import com.barboya.orders.OrderDtos.ReasonRequest;
import com.barboya.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courier")
@PreAuthorize("hasRole('COURIER')")
public class CourierDeliveryController {

    private final CourierDeliveryService service;

    public CourierDeliveryController(CourierDeliveryService service) {
        this.service = service;
    }

    @GetMapping("/offers")
    public List<DeliveryOffer> offers(@AuthenticationPrincipal AuthUser user) {
        return service.offers(user.id());
    }

    @PostMapping("/offers/{id}/accept")
    public OrderResponse accept(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return service.accept(user.id(), id);
    }

    /** Responde 204 cuando no hay ninguna entrega en curso. */
    @GetMapping("/delivery")
    public ResponseEntity<OrderResponse> current(@AuthenticationPrincipal AuthUser user) {
        OrderResponse order = service.current(user.id());
        return order == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(order);
    }

    @PostMapping("/delivery/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                        @Valid @RequestBody ReasonRequest body) {
        service.release(user.id(), id, body.reason());
    }

    @PostMapping("/delivery/{id}/pickup")
    public OrderResponse pickUp(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                @Valid @RequestBody PickupRequest body) {
        return service.pickUp(user.id(), id, body.code());
    }

    @PostMapping(path = "/delivery/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrderResponse evidence(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                  @RequestPart("file") MultipartFile file) {
        return service.addEvidence(user.id(), id, file);
    }

    @GetMapping("/delivery/{id}/evidence")
    public ResponseEntity<byte[]> viewEvidence(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return EvidenceResponses.of(service.evidence(user.id(), id));
    }

    @PostMapping("/delivery/{id}/deliver")
    public OrderResponse deliver(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                 @Valid @RequestBody DeliverRequest body) {
        return service.deliver(user.id(), id, body.pin());
    }

    @PostMapping("/delivery/{id}/fail")
    public OrderResponse fail(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                              @Valid @RequestBody ReasonRequest body) {
        return service.fail(user.id(), id, body.reason());
    }

    @GetMapping("/history")
    public OrderPage history(@AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "0") int page) {
        return service.history(user.id(), page);
    }

    @GetMapping("/earnings")
    public Earnings earnings(@AuthenticationPrincipal AuthUser user) {
        return service.earnings(user.id());
    }
}
