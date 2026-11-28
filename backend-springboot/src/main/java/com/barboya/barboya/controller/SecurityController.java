package com.barboya.barboya.controller;

import com.barboya.barboya.dto.SecurityDtos;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.SecurityService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/security", "/api/security"})
public class SecurityController {
    private final SecurityService sec;

    public SecurityController(SecurityService sec) {
        this.sec = sec;
    }

    @PostMapping("/sos")
    public Object sos(@RequestBody SecurityDtos.SosRequest req) {
        return sec.log(req.tripId(), CurrentUser.id(), com.barboya.barboya.enums.Enums.SecurityEventType.sos, req.details(), req.lat(), req.lng());
    }

    @PostMapping("/stopped")
    public Object stopped(@RequestBody SecurityDtos.StoppedRequest req) {
        return sec.log(req.tripId(), CurrentUser.id(), com.barboya.barboya.enums.Enums.SecurityEventType.stopped_too_long, "", req.lat(), req.lng());
    }

    @PostMapping("/trips/{id}/share-link")
    public SecurityDtos.ShareLinkResponse share(@PathVariable UUID id) {
        return new SecurityDtos.ShareLinkResponse(sec.shareLink(id));
    }
}