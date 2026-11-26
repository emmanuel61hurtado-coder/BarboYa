package com.barboya.barboya.controller;

import com.barboya.barboya.model.WalletTransaction;
import com.barboya.barboya.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/admin/wallet", "/api/admin/wallet"})
public class WalletAdminController {
    private final WalletService wallet;

    public WalletAdminController(WalletService wallet) {
        this.wallet = wallet;
    }

    @PostMapping("/topups/{id}/decision")
    public WalletTransaction decide(@PathVariable UUID id, @RequestParam boolean approve) {
        return wallet.decide(id, approve);
    }
}