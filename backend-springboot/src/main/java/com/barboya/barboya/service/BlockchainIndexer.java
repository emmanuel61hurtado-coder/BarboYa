package com.barboya.barboya.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BlockchainIndexer {
    private final BlockchainService chain;
    private final com.barboya.barboya.config.BarboyaProperties props;

    public BlockchainIndexer(BlockchainService chain, com.barboya.barboya.config.BarboyaProperties props) {
        this.chain = chain;
        this.props = props;
    }

    @Scheduled(fixedDelay = 15000)
    public void run() {
        if (props.getPaymentTokenAddress() == null || props.getPaymentTokenAddress().isBlank()) {
            return;
        }
        chain.syncPending();
    }
}