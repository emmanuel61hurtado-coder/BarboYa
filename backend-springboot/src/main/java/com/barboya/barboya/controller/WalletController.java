package com.barboya.barboya.controller;

import com.barboya.barboya.dto.WalletDtos;
import com.barboya.barboya.model.WalletTransaction;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/wallet", "/api/wallet"})
public class WalletController {
    private final WalletService wallet;

    public WalletController(WalletService wallet) {
        this.wallet = wallet;
    }

    @PostMapping("/topup")
    public WalletTransaction topup(@RequestBody WalletDtos.TopupRequest req) {
        return wallet.requestTopup(CurrentUser.id(), req);
    }

    @GetMapping("/transactions")
    public List<WalletTransaction> txs() {
        return wallet.list(CurrentUser.id());
    }
}