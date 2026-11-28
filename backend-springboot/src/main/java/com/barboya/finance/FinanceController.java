package com.barboya.finance;

import com.barboya.finance.FinanceDtos.Statement;
import com.barboya.security.AuthUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estado de cuenta propio. */
@RestController
@RequestMapping("/api/v1/finance")
@PreAuthorize("hasAnyRole('RESTAURANT','STORE','COURIER')")
public class FinanceController {

    private final LedgerService ledger;

    public FinanceController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @GetMapping("/statement")
    public Statement statement(@AuthenticationPrincipal AuthUser user,
                               @RequestParam(defaultValue = "0") int page) {
        return ledger.statementOf(user.id(), page);
    }
}
