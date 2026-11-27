package com.barboya.finance;

import com.barboya.common.ClientInfo;
import com.barboya.finance.FinanceDtos.AdjustmentRequest;
import com.barboya.finance.FinanceDtos.PaymentRequest;
import com.barboya.finance.FinanceDtos.ReversalRequest;
import com.barboya.finance.FinanceDtos.Statement;
import com.barboya.finance.FinanceDtos.Summary;
import com.barboya.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Perdonar deudas y reversar movimientos queda reservado al superadministrador. */
@RestController
@RequestMapping("/api/v1/admin/finance")
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminFinanceController {

    private final LedgerService ledger;

    public AdminFinanceController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/summary")
    public Summary summary() {
        return ledger.summary();
    }

    @GetMapping("/accounts/{partyId}")
    public Statement statement(@PathVariable UUID partyId, @RequestParam(defaultValue = "0") int page) {
        return ledger.statementOf(partyId, page);
    }

    @PostMapping("/accounts/{partyId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Statement payment(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID partyId,
                             @Valid @RequestBody PaymentRequest body, HttpServletRequest request) {
        return ledger.registerPayment(actor.id(), partyId, body, ClientInfo.from(request));
    }

    @PostMapping("/accounts/{partyId}/adjustments")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Statement adjustment(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID partyId,
                                @Valid @RequestBody AdjustmentRequest body, HttpServletRequest request) {
        return ledger.waive(actor.id(), partyId, body, ClientInfo.from(request));
    }

    @PostMapping("/entries/{entryId}/reversal")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Statement reversal(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID entryId,
                              @Valid @RequestBody ReversalRequest body, HttpServletRequest request) {
        return ledger.reverse(actor.id(), entryId, body.reason(), ClientInfo.from(request));
    }
}
