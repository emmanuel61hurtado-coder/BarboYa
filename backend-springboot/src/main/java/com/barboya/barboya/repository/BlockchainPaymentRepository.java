package com.barboya.barboya.repository;

import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.BlockchainPayment;
import com.barboya.barboya.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlockchainPaymentRepository extends JpaRepository<BlockchainPayment, UUID> {
    List<BlockchainPayment> findByUser(User user);
    List<BlockchainPayment> findByStatus(Enums.BlockchainTxStatus status);
}