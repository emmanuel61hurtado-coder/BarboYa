package com.barboya.businesses;

import com.barboya.businesses.BusinessDtos.BusinessResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Datos del comercio que el equipo revisa antes de aprobar la cuenta. */
@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class BusinessReviewController {

    private final BusinessService service;

    public BusinessReviewController(BusinessService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/users/{ownerId}/business")
    public BusinessResponse business(@PathVariable UUID ownerId) {
        return service.owned(ownerId);
    }
}
