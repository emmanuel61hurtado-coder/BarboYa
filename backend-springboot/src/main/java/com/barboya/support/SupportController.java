package com.barboya.support;

import com.barboya.security.AuthUser;
import com.barboya.support.SupportDtos.NewTicket;
import com.barboya.support.SupportDtos.Reply;
import com.barboya.support.SupportDtos.TicketPage;
import com.barboya.support.SupportDtos.TicketView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Casos propios de cualquier persona con sesion. */
@RestController
@RequestMapping("/api/v1/support/tickets")
public class SupportController {

    private final SupportService support;

    public SupportController(SupportService support) {
        this.support = support;
    }

    @GetMapping
    public TicketPage mine(@AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "0") int page) {
        return support.mine(user.id(), page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketView open(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody NewTicket body) {
        return support.open(user.id(), body);
    }

    @GetMapping("/{id}")
    public TicketView get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return support.own(user.id(), id);
    }

    @PostMapping("/{id}/messages")
    public TicketView reply(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                            @Valid @RequestBody Reply body) {
        return support.reply(user.id(), id, body.body());
    }
}
