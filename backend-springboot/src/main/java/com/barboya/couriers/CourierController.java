package com.barboya.couriers;

import com.barboya.common.ClientInfo;
import com.barboya.couriers.CourierDtos.CourierView;
import com.barboya.couriers.CourierDtos.ProfileRequest;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courier")
@PreAuthorize("hasRole('COURIER')")
public class CourierController {

    private final CourierService service;

    public CourierController(CourierService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public CourierView profile(@AuthenticationPrincipal AuthUser user) {
        return service.view(user.id());
    }

    @PutMapping("/profile")
    public CourierView save(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ProfileRequest body,
                            HttpServletRequest request) {
        return service.saveProfile(user.id(), body, ClientInfo.from(request));
    }

    @PostMapping(path = "/documents/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourierView upload(@AuthenticationPrincipal AuthUser user, @PathVariable DocumentType type,
                              @RequestPart("file") MultipartFile file,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                              HttpServletRequest request) {
        return service.upload(user.id(), type, file, expiresOn, ClientInfo.from(request));
    }

    @DeleteMapping("/documents/{id}")
    public CourierView delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                              HttpServletRequest request) {
        return service.delete(user.id(), id, ClientInfo.from(request));
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<byte[]> file(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return FileResponses.of(service.ownFile(user.id(), id));
    }
}
