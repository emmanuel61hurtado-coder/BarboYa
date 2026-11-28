package com.barboya.addresses;

import com.barboya.addresses.AddressService.AddressRequest;
import com.barboya.addresses.AddressService.AddressResponse;
import com.barboya.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Direcciones propias: solo quien las guardo puede verlas. */
@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal AuthUser user) {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse add(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddressRequest body) {
        return service.add(user.id(), body);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                  @Valid @RequestBody AddressRequest body) {
        return service.update(user.id(), id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        service.remove(user.id(), id);
    }
}
