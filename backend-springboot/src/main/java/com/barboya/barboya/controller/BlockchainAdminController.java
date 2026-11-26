package com.barboya.barboya.controller;

import com.barboya.barboya.model.BlockchainPayment;
import com.barboya.barboya.service.BlockchainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/admin/blockchain", "/api/admin/blockchain"})
public class BlockchainAdminController {
    private final BlockchainService chain;

    public BlockchainAdminController(BlockchainService chain) {
        this.chain = chain;
    }

    @PostMapping("/sync")
    public List<BlockchainPayment> sync() {
        return chain.syncPending();
    }
}