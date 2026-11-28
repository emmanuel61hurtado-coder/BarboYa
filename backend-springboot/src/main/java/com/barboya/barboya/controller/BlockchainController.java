package com.barboya.barboya.controller;

import com.barboya.barboya.dto.BlockchainDtos;
import com.barboya.barboya.model.BlockchainPayment;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.BlockchainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/payments", "/api/payments"})
public class BlockchainController {
    private final BlockchainService chain;

    public BlockchainController(BlockchainService chain) {
        this.chain = chain;
    }

    @PostMapping("/crypto")
    public BlockchainPayment report(@RequestBody BlockchainDtos.CryptoPaymentRequest req) {
        return chain.report(CurrentUser.id(), req);
    }

    @GetMapping("/crypto")
    public List<BlockchainPayment> list() {
        return chain.list(CurrentUser.id());
    }
}