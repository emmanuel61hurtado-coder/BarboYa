package com.barboya.barboya.service;

import com.barboya.barboya.dto.BlockchainDtos;
import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.BlockchainPayment;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.BlockchainPaymentRepository;
import com.barboya.barboya.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BlockchainService {
    private final BlockchainPaymentRepository payments;
    private final UserRepository users;
    private final com.barboya.barboya.config.BarboyaProperties props;

    public BlockchainService(BlockchainPaymentRepository payments, UserRepository users, com.barboya.barboya.config.BarboyaProperties props) {
        this.payments = payments;
        this.users = users;
        this.props = props;
    }

    public BlockchainPayment report(UUID userId, BlockchainDtos.CryptoPaymentRequest req) {
        User u = users.findById(userId).orElseThrow();
        BlockchainPayment p = new BlockchainPayment();
        p.setUser(u);
        p.setTxHash(req.txHash());
        p.setPayerAddress(req.payerAddress());
        p.setTokenAmountUnits(req.tokenAmountUnits());
        p.setAmountCop((int) (req.tokenAmountUnits() / 1_000_000.0 * props.getCopPerToken()));
        p.setStatus(Enums.BlockchainTxStatus.pending);
        return payments.save(p);
    }

    public List<BlockchainPayment> list(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        return payments.findByUser(u);
    }

    public List<BlockchainPayment> syncPending() {
        List<BlockchainPayment> pending = payments.findByStatus(Enums.BlockchainTxStatus.pending);
        for (BlockchainPayment p : pending) {
            // simulated chain: move to confirmed
            p.setStatus(Enums.BlockchainTxStatus.confirmed);
            p.setConfirmations(props.getRequiredConfirmations());
            p.setConfirmedAt(Instant.now());
            payments.save(p);
        }
        return pending;
    }
}