package com.barboya.support;

import com.barboya.common.ClientInfo;
import com.barboya.security.AuthUser;
import com.barboya.support.SupportDtos.Reply;
import com.barboya.support.SupportDtos.Resolution;
import com.barboya.support.SupportDtos.TicketPage;
import com.barboya.support.SupportDtos.TicketView;
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
@RequestMapping("/api/v1/admin/support/tickets")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminSupportController {

    private final SupportService support;

    public AdminSupportController(SupportService support) {
        this.support = support;
    }

    @GetMapping
    public TicketPage queue(@RequestParam(defaultValue = "OPEN") Ticket.Status status,
                            @RequestParam(defaultValue = "0") int page) {
        return support.queue(status, page);
    }

    @GetMapping("/{id}")
    public TicketView get(@PathVariable UUID id) {
        return support.forStaff(id);
    }

    @PostMapping("/{id}/messages")
    public TicketView reply(@AuthenticationPrincipal AuthUser staff, @PathVariable UUID id,
                            @Valid @RequestBody Reply body) {
        return support.staffReply(staff.id(), id, body.body());
    }

    @PostMapping("/{id}/resolve")
    public TicketView resolve(@AuthenticationPrincipal AuthUser staff, @PathVariable UUID id,
                              @Valid @RequestBody Resolution body, HttpServletRequest request) {
        return support.resolve(staff.id(), id, body.resolution(), ClientInfo.from(request));
    }
}
