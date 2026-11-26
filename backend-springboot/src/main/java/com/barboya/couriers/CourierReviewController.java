package com.barboya.couriers;

import com.barboya.common.ClientInfo;
import com.barboya.couriers.CourierDtos.CourierView;
import com.barboya.couriers.CourierDtos.ReviewRequest;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Revision humana de la documentacion. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class CourierReviewController {

    private final CourierService service;

    public CourierReviewController(CourierService service) {
        this.service = service;
    }

    @GetMapping("/users/{userId}/courier")
    public CourierView courier(@PathVariable UUID userId) {
        return service.view(userId);
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<byte[]> file(@AuthenticationPrincipal AuthUser reviewer, @PathVariable UUID id,
                                       HttpServletRequest request) {
        return FileResponses.of(service.reviewFile(reviewer.id(), id, ClientInfo.from(request)));
    }

    @PostMapping("/documents/{id}/review")
    public CourierView review(@AuthenticationPrincipal AuthUser reviewer, @PathVariable UUID id,
                              @Valid @RequestBody ReviewRequest body, HttpServletRequest request) {
        return service.review(reviewer.id(), id, body, ClientInfo.from(request));
    }
}
