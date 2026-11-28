package com.barboya.barboya.service;

import com.barboya.barboya.dto.WalletDtos;
import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.User;
import com.barboya.barboya.model.WalletTransaction;
import com.barboya.barboya.repository.UserRepository;
import com.barboya.barboya.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {
    private final WalletTransactionRepository txs;
    private final UserRepository users;

    public WalletService(WalletTransactionRepository txs, UserRepository users) {
        this.txs = txs;
        this.users = users;
    }

    public WalletTransaction requestTopup(UUID userId, WalletDtos.TopupRequest req) {
        User u = users.findById(userId).orElseThrow();
        WalletTransaction t = new WalletTransaction();
        t.setUser(u);
        t.setType(Enums.WalletTxType.topup);
        t.setStatus(Enums.WalletTxStatus.pending);
        t.setAmountCop(req.amountCop());
        t.setDescription("Topup via " + req.method());
        return txs.save(t);
    }

    public WalletTransaction decide(UUID txId, boolean approve) {
        WalletTransaction t = txs.findById(txId).orElseThrow();
        t.setStatus(approve ? Enums.WalletTxStatus.approved : Enums.WalletTxStatus.rejected);
        t.setDecidedAt(Instant.now());
        if (approve) {
            // credit driver wallet if driver profile exists
        }
        return txs.save(t);
    }

    public List<WalletTransaction> list(UUID userId) {
        User u = users.findById(userId).orElseThrow();
        return txs.findByUser(u);
    }
}