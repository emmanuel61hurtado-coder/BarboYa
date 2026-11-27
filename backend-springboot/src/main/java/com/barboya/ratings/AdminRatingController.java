package com.barboya.ratings;

import com.barboya.common.ClientInfo;
import com.barboya.ratings.RatingDtos.Moderated;
import com.barboya.ratings.RatingDtos.ModeratedPage;
import com.barboya.ratings.RatingDtos.Moderation;
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
@RequestMapping("/api/v1/admin/ratings")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminRatingController {

    private final RatingService service;

    public AdminRatingController(RatingService service) {
        this.service = service;
    }

    @GetMapping
    public ModeratedPage list(@RequestParam(defaultValue = "false") boolean hidden,
                              @RequestParam(defaultValue = "0") int page) {
        return service.moderationList(hidden, page);
    }

    @PostMapping("/{id}/hide")
    public Moderated hide(@AuthenticationPrincipal AuthUser staff, @PathVariable UUID id,
                          @Valid @RequestBody Moderation body, HttpServletRequest request) {
        return service.hide(staff.id(), id, body.reason(), ClientInfo.from(request));
    }

    @PostMapping("/{id}/restore")
    public Moderated restore(@AuthenticationPrincipal AuthUser staff, @PathVariable UUID id,
                             @Valid @RequestBody Moderation body, HttpServletRequest request) {
        return service.restore(staff.id(), id, body.reason(), ClientInfo.from(request));
    }
}
